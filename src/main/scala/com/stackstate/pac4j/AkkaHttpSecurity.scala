package com.stackstate.pac4j

import java.util

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{ Error, Ok }
import akka.http.scaladsl.model.{ HttpHeader, HttpResponse }
import akka.http.scaladsl.server.Directives.{ authorize => akkaHttpAuthorize }
import akka.http.scaladsl.server.{ Directive0, Directive1, Route, RouteResult }
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.RouteResult.Complete
import com.stackstate.pac4j.AkkaHttpSecurity.AkkaHttpSecurityLogic
import com.stackstate.pac4j.AkkaHttpWebContext.ResponseChanges
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import org.pac4j.core.authorization.authorizer.Authorizer
import org.pac4j.core.config.Config
import org.pac4j.core.engine.{ DefaultSecurityLogic, SecurityLogic }
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.context.Cookie

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.immutable

object AkkaHttpSecurity {
  type AkkaHttpSecurityLogic = SecurityLogic[Future[RouteResult], AkkaHttpWebContext]

  def authorize(authorizer: Authorizer[CommonProfile])(request: AuthenticatedRequest): Directive0 =
    akkaHttpAuthorize(authorizer.isAuthorized(request.webContext, request.profiles.asJava))

  private def mapCookie(cookie: Cookie): HttpCookie = {
    HttpCookie(
      name = cookie.getName,
      value = cookie.getValue,
      expires = None,
      maxAge = if (cookie.getMaxAge < 0) None else Some(cookie.getMaxAge),
      domain = Option(cookie.getDomain),
      path = Option(cookie.getPath),
      secure = cookie.isSecure,
      httpOnly = cookie.isHttpOnly,
      extension = None)
  }

  private def applyHeadersAndCookiesToResponse(changes: ResponseChanges)(httpResponse: HttpResponse): HttpResponse = {
    val regularHeaders: List[HttpHeader] =
      changes.headers
        .map({
          case (name, value) => HttpHeader.parse(name, value) match {
            case Ok(header, _) => header
            case Error(error) => throw new IllegalArgumentException(s"Error parsing http header ${error.formatPretty}")
          }
        })

    val cookieHeaders: List[HttpHeader] = changes.cookies.map(mapCookie).map(v => `Set-Cookie`(v))
    val additionalHeaders: immutable.Seq[HttpHeader] = regularHeaders ++ cookieHeaders

    httpResponse.mapHeaders(additionalHeaders ++ _)
  }
}

class AkkaHttpSecurity[P <: CommonProfile](config: Config)(implicit val executionContext: ExecutionContext) {

  import AkkaHttpSecurity._
  // TODO: At some point this object should contain the SessionStore (when we implement that)

  val securityLogic: AkkaHttpSecurityLogic =
    Option(config.getSecurityLogic) match {
      case Some(v) => v.asInstanceOf[AkkaHttpSecurityLogic]
      case None => new DefaultSecurityLogic[Future[RouteResult], AkkaHttpWebContext]
    }

  val actionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext] =
    Option(config.getHttpActionAdapter) match {
      case Some(v) => v.asInstanceOf[HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext]]
      case None => AkkaHttpActionAdapter
    }

  /**
   * Authenticate using the provided pac4j configuration. Delivers an AuthenticationRequest which can be used for further authorization
   * this does not apply any authorization ofr filtering.
   */
  def withAuthentication(
    clients: String = null /* Default null, meaning all defined clients */ ,
    multiProfile: Boolean = true): Directive1[AuthenticatedRequest] =
    new Directive1[AuthenticatedRequest] {
      override def tapply(innerRoute: Tuple1[AuthenticatedRequest] => Route): Route = { ctx =>
        val context = AkkaHttpWebContext(ctx.request)
        securityLogic.perform(context, config, (context: AkkaHttpWebContext, profiles: util.Collection[CommonProfile], parameters: AnyRef) => {
          val authenticatedRequest = AuthenticatedRequest(context, profiles.asScala.toList)
          innerRoute(Tuple1(authenticatedRequest))(ctx)
        }, actionAdapter, clients, "", "", multiProfile).map[RouteResult] {
          case Complete(response) => Complete(applyHeadersAndCookiesToResponse(context.getChanges)(response))
          case rejection => rejection
        }
      }
    }
}
