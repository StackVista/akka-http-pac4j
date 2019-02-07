package com.stackstate.pac4j.authorizer

import java.util.UUID

import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.{Cookie, Pac4jConstants}

import scala.concurrent.duration.FiniteDuration

object CsrfCookieAuthorizer {

  val CookiePath = "/"

  def apply(context: AkkaHttpWebContext, maxAge: Option[FiniteDuration]): AkkaHttpWebContext = {
    val token = UUID.randomUUID.toString
    val cookie = new Cookie(Pac4jConstants.CSRF_TOKEN, token)
    cookie.setPath(CookiePath)

    maxAge.map(_.toSeconds.toInt).foreach {
      cookie.setMaxAge
    }

    context.setRequestAttribute(Pac4jConstants.CSRF_TOKEN, token)
    context.getSessionStore.set(context, Pac4jConstants.CSRF_TOKEN, token)
    context.addResponseCookie(cookie)

    context
  }

}
