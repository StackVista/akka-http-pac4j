package com.stackstate.pac4j.authorizer

import java.util.UUID

import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.Cookie

import scala.concurrent.duration.FiniteDuration
import org.pac4j.core.util.Pac4jConstants

object CsrfCookieAuthorizer {

  val CookiePath = "/"

  def apply(context: AkkaHttpWebContext, maxAge: Option[FiniteDuration]): AkkaHttpWebContext = {
    val token = UUID.randomUUID.toString

    val cookieWithDomain = createCookie(token, maxAge)
    cookieWithDomain.setDomain(context.getServerName)

    val cookieWithoutDomain = createCookie(token, maxAge)

    context.setRequestAttribute(Pac4jConstants.CSRF_TOKEN, token)
    context.getSessionStore.set(context, Pac4jConstants.CSRF_TOKEN, token)

    // previous versions set both cookies. This change is to keep it backwards compatible.
    context.addResponseCookie(cookieWithDomain)
    context.addResponseCookie(cookieWithoutDomain)
    context
  }

  def createCookie(token: String, maxAge: Option[FiniteDuration]) = {
    val cookie = new Cookie(Pac4jConstants.CSRF_TOKEN, token)
    cookie.setPath(CookiePath)

    maxAge.map(_.toSeconds.toInt).foreach {
      cookie.setMaxAge
    }
    cookie
  }

}
