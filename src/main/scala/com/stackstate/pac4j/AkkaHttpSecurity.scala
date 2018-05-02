package com.stackstate.pac4j

import java.util

import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpResponse}
import akka.http.scaladsl.server.Directives.{authorize => akkaHttpAuthorize}
import akka.http.scaladsl.server.{Directive0, Directive1, Route, RouteResult}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.stackstate.pac4j.AkkaHttpWebContext.ResponseChanges
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import org.pac4j.core.authorization.authorizer.Authorizer
import org.pac4j.core.config.Config
import org.pac4j.core.engine.{CallbackLogic, DefaultCallbackLogic, DefaultSecurityLogic, SecurityLogic}
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.profile.CommonProfile
import akka.http.scaladsl.util.FastFuture._
import akka.stream.Materializer
import com.stackstate.pac4j.store.SessionStorage
import org.pac4j.core.context.Pac4jConstants

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable

object AkkaHttpSecurity {
  type AkkaHttpSecurityLogic = SecurityLogic[Future[RouteResult], AkkaHttpWebContext]
  type AkkaHttpCallbackLogic = CallbackLogic[Future[RouteResult], AkkaHttpWebContext]

  def authorize(authorizer: Authorizer[CommonProfile])(request: AuthenticatedRequest): Directive0 =
    akkaHttpAuthorize(authorizer.isAuthorized(request.webContext, request.profiles.asJava))

  private def applyHeadersAndCookiesToResponse(changes: ResponseChanges)(httpResponse: HttpResponse): HttpResponse = {
    val regularHeaders: List[HttpHeader] = changes.headers
    val cookieHeaders: List[HttpHeader] = changes.cookies.map(v => `Set-Cookie`(v))
    val additionalHeaders: immutable.Seq[HttpHeader] = regularHeaders ++ cookieHeaders

    httpResponse.mapHeaders(additionalHeaders ++ _)
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
        Future.successful(Seq.empty)
      }
    }
  }
}

class AkkaHttpSecurity(config: Config, sessionStorage: SessionStorage)(implicit val executionContext: ExecutionContext) {

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
      case Some(v) => v.asInstanceOf[CallbackLogic[Future[RouteResult], AkkaHttpWebContext]]
      case None => new DefaultCallbackLogic[Future[RouteResult], AkkaHttpWebContext]
    }

  /**
    * This directive constructs a pac4j context for a route. This means the request is interpreted into
    * an AkkaHttpWebContext and any changes to this context are applied when the route returns (e.g. headers/cookies).
    */
  private[pac4j] def withContext[A](enforceFormEncoding: Boolean): Directive1[AkkaHttpWebContext] =
    new Directive1[AkkaHttpWebContext] {
      override def tapply(innerRoute: Tuple1[AkkaHttpWebContext] => Route): Route = { ctx =>
        import ctx.materializer

        getFormFields(ctx.request.entity, enforceFormEncoding).flatMap { formParams =>
          val akkaWebContext = AkkaHttpWebContext(ctx.request, formParams, sessionStorage)
          innerRoute(Tuple1(akkaWebContext))(ctx).map[RouteResult] {
            case Complete(response) => Complete(applyHeadersAndCookiesToResponse(akkaWebContext.getChanges)(response))
            case rejection => rejection
          }
        }
      }
    }

  /**
    * Authenticate using the provided pac4j configuration. Delivers an AuthenticationRequest which can be used for further authorization
    * this does not apply any authorization ofr filtering.
    */
  def withAuthentication(
                          clients: String = null /* Default null, meaning all defined clients */ ,
                          multiProfile: Boolean = true,
                          enforceFormEncoding: Boolean = false, //Force form parameters to be passed for authentication or the request fails
                        ): Directive1[AuthenticatedRequest] =
    withContext(enforceFormEncoding).flatMap { akkaWebContext =>
      new Directive1[AuthenticatedRequest] {
        override def tapply(innerRoute: Tuple1[AuthenticatedRequest] => Route): Route = { ctx =>
          securityLogic.perform(akkaWebContext, config, (context: AkkaHttpWebContext, profiles: util.Collection[CommonProfile], parameters: AnyRef) => {
            val authenticatedRequest = AuthenticatedRequest(context, profiles.asScala.toList)
            innerRoute(Tuple1(authenticatedRequest))(ctx)
          }, actionAdapter, clients, "", "", multiProfile)
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
               enforceFormEncoding: Boolean = false
              ): Route = {
    withContext(enforceFormEncoding) { akkaWebContext => ctx =>
      callbackLogic.perform(akkaWebContext, config, actionAdapter, defaultUrl, saveInSession, multiProfile, false, defaultClient.getOrElse(null))
    }
  }

  def withAllClientsAuthentication(multiProfile: Boolean = true, enforceFormEncoding: Boolean = false): Directive1[AuthenticatedRequest] =
    withAuthentication(config.getClients.findAllClients().asScala.map(_.getName).mkString(","), multiProfile, enforceFormEncoding)
}
