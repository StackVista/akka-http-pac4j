package com.stackstate.pac4j

import java.util

import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpResponse}
import akka.http.scaladsl.server.Directives.{authorize => akkaHttpAuthorize}
import akka.http.scaladsl.server.{Directive, Directive0, Directive1, Route, RouteResult}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.stackstate.pac4j.AkkaHttpWebContext.ResponseChanges
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import org.pac4j.core.authorization.authorizer.Authorizer
import org.pac4j.core.config.Config
import org.pac4j.core.engine._
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.profile.CommonProfile
import akka.http.scaladsl.util.FastFuture._
import akka.stream.Materializer
import com.stackstate.pac4j.store.SessionStorage
import org.pac4j.core.context.Pac4jConstants

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable

object AkkaHttpSecurity {
  type AkkaHttpSecurityLogic = SecurityLogic[Future[RouteResult], AkkaHttpWebContext]
  type AkkaHttpCallbackLogic = CallbackLogic[Future[RouteResult], AkkaHttpWebContext]
  type AkkaHttpLogoutLogic = LogoutLogic[Future[RouteResult], AkkaHttpWebContext]

  def authorize(authorizer: Authorizer[CommonProfile])(request: AuthenticatedRequest): Directive0 =
    akkaHttpAuthorize(authorizer.isAuthorized(request.webContext, request.profiles.asJava))

  private def applyHeadersAndCookiesToResponse(changes: ResponseChanges)(httpResponse: HttpResponse): HttpResponse = {
    val regularHeaders: List[HttpHeader] = changes.headers
    val cookieHeaders: List[HttpHeader] = changes.cookies.map(v => `Set-Cookie`(v))
    val additionalHeaders: immutable.Seq[HttpHeader] = regularHeaders ++ cookieHeaders

    httpResponse.mapHeaders(h => (additionalHeaders ++ h).distinct)
  }

  /**
    * Try to extract authentication credentials from form parameters using Akka's StrictForm unmarshallers.
    * If that fails, either form encoding is enforced, in which case the request fails, or the request proceeds.
    * If the request proceeds, other ways (e.g. basic auth) are assumed to be configured in pac4j in order to pass
    * credentials.
    */
  private def getFormFields(entity: HttpEntity, enforceFormEncoding: Boolean)(implicit materializer: Materializer, executionContext: ExecutionContext): Future[Seq[(String, String)]] = {
    Unmarshal(entity).to[StrictForm].fast.flatMap { form =>
      val fields = form.fields.collect {
        case (name, field) if name.nonEmpty =>
          Unmarshal(field).to[String].map(fieldString => (name, fieldString))
      }
      Future.sequence(fields)
    }.recoverWith { case e =>
      if (enforceFormEncoding) {
        Future.failed(e)
      } else {
        Future.successful(immutable.Seq.empty)
      }
    }
  }
}

class AkkaHttpSecurity(config: Config, sessionStorage: SessionStorage, val sessionCookieName: String = AkkaHttpWebContext.DEFAULT_COOKIE_NAME)(implicit val executionContext: ExecutionContext) {

  import AkkaHttpSecurity._

  private[pac4j] val securityLogic: AkkaHttpSecurityLogic =
    Option(config.getSecurityLogic) match {
      case Some(v) => v.asInstanceOf[AkkaHttpSecurityLogic]
      case None => new DefaultSecurityLogic[Future[RouteResult], AkkaHttpWebContext]
    }

  private[pac4j] val actionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext] =
    Option(config.getHttpActionAdapter) match {
      case Some(v) => v.asInstanceOf[HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext]]
      case None => AkkaHttpActionAdapter
    }

  private[pac4j] val callbackLogic: CallbackLogic[Future[RouteResult], AkkaHttpWebContext] =
    Option(config.getCallbackLogic) match {
      case Some(v) => v.asInstanceOf[AkkaHttpCallbackLogic]
      case None => new DefaultCallbackLogic[Future[RouteResult], AkkaHttpWebContext]
    }

  private[pac4j] val logoutLogic: LogoutLogic[Future[RouteResult], AkkaHttpWebContext] =
    Option(config.getLogoutLogic) match {
      case Some(v) => v.asInstanceOf[AkkaHttpLogoutLogic]
      case None => new DefaultLogoutLogic[Future[RouteResult], AkkaHttpWebContext]
    }

  /**
    * This directive constructs a pac4j context for a route. This means the request is interpreted into
    * an AkkaHttpWebContext and any changes to this context are applied when the route returns (e.g. headers/cookies).
    */
  def withContext(existingContext: Option[AkkaHttpWebContext] = None, formParams: Map[String, String] = Map.empty): Directive1[AkkaHttpWebContext] =
    Directive[Tuple1[AkkaHttpWebContext]] { inner => ctx =>
      val akkaWebContext = existingContext.getOrElse(AkkaHttpWebContext(ctx.request, formParams.toSeq, sessionStorage, sessionCookieName = sessionCookieName))
      inner(Tuple1(akkaWebContext))(ctx).map[RouteResult] {
        case Complete(response) => Complete(applyHeadersAndCookiesToResponse(akkaWebContext.getChanges)(response))
        case rejection => rejection
      }
    }

  def withFormParameters(enforceFormEncoding: Boolean): Directive1[Map[String, String]] =
    Directive[Tuple1[Map[String, String]]] { inner => ctx =>
      import ctx.materializer
      getFormFields(ctx.request.entity, enforceFormEncoding).flatMap { params =>
        inner(Tuple1(params.toMap))(ctx)
      }
    }

  /**
    * Authenticate using the provided pac4j configuration. Delivers an AuthenticationRequest which can be used for further authorization
    * this does not apply any authorization ofr filtering.
    */
  def withAuthentication(
                          clients: String = null /* Default null, meaning all defined clients */ ,
                          multiProfile: Boolean = true,
                          authorizers: String = ""
                        ): Directive1[AuthenticatedRequest] =
    withContext().flatMap { akkaWebContext =>
      Directive[Tuple1[AuthenticatedRequest]] { inner => ctx =>
        // TODO This is a hack to ensure that any underlying Futures are scheduled (and handled in case of errors) from here
        // TODO Fix this properly
        Future.successful(()).flatMap { _ =>
          val securityGrantedAccessAdapter = new SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext] {
            override def adapt(context: AkkaHttpWebContext, profiles: util.Collection[CommonProfile], parameters: AnyRef*): Future[RouteResult] = {
              val authenticatedRequest = AuthenticatedRequest(context, profiles.asScala.toList)
              inner(Tuple1(authenticatedRequest))(ctx)
            }
          }
          securityLogic.perform(akkaWebContext, config, securityGrantedAccessAdapter, actionAdapter, clients, authorizers, "", multiProfile)
        }
      }
    }

  /**
    * Callback to finish the login process for indirect clients.
    */
  def callback(defaultUrl: String = Pac4jConstants.DEFAULT_URL_VALUE,
               saveInSession: Boolean = true,
               multiProfile: Boolean = true,
               defaultClient: Option[String] = None,
               enforceFormEncoding: Boolean = false,
               existingContext: Option[AkkaHttpWebContext] = None,
               setCsrfCookie: Boolean = true
              ): Route =
  withFormParameters(enforceFormEncoding) { formParams =>
    withContext(existingContext, formParams) { akkaWebContext => _ =>
      callbackLogic.perform(akkaWebContext, config, actionAdapter, defaultUrl, saveInSession, multiProfile, true, defaultClient.orNull)
        .map { result =>
          akkaWebContext.addResponseSessionCookie()
          if (setCsrfCookie) akkaWebContext.addResponseCsrfCookie()
          result
        }
    }
  }

  def logout(defaultUrl: String = Pac4jConstants.DEFAULT_URL_VALUE,
             logoutPatternUrl: String = Pac4jConstants.DEFAULT_LOGOUT_URL_PATTERN_VALUE,
             localLogout: Boolean = true,
             destroySession: Boolean = true,
             centralLogout: Boolean = false
            ): Route = {
    withContext() { akkaWebContext => ctx =>
        logoutLogic.perform(akkaWebContext, config, actionAdapter, defaultUrl, logoutPatternUrl, localLogout, destroySession, centralLogout)
    }
  }

  def withAllClientsAuthentication(multiProfile: Boolean = true): Directive1[AuthenticatedRequest] = {
    val authorizers = config.getAuthorizers.keySet().asScala.mkString(",")
    val clients = config.getClients.findAllClients().asScala.map(_.getName).mkString(",")
    withAuthentication(clients, multiProfile, authorizers)
  }
}
