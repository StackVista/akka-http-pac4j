package com.stackstate.pac4j.http

import java.util.Optional
import compat.java8.OptionConverters._
import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore

class AkkaHttpSessionStore() extends SessionStore {
  override def getSessionId(context: WebContext, createSession: Boolean): Optional[String] = {
    if (createSession) {
      Optional.of(context.asInstanceOf[AkkaHttpWebContext].getOrCreateSessionId())
    } else {
      context.asInstanceOf[AkkaHttpWebContext].getSessionId.asJava
    }

  }

  override def get(context: WebContext, key: String): Optional[Object] =
    context.asInstanceOf[AkkaHttpWebContext].getSessionId match {
      case Some(value) => context.asInstanceOf[AkkaHttpWebContext].sessionStorage.getSessionValue(value, key).asJava
      case None => Optional.empty()
    }

  override def set(context: WebContext, key: String, value: scala.AnyRef): Unit = {
    context
      .asInstanceOf[AkkaHttpWebContext]
      .sessionStorage
      .setSessionValue(context.asInstanceOf[AkkaHttpWebContext].getOrCreateSessionId(), key, value)
    ()
  }

  override def destroySession(context: WebContext): Boolean = context.asInstanceOf[AkkaHttpWebContext].destroySession()

  override def getTrackableSession(context: WebContext): Optional[AnyRef] =
    context.asInstanceOf[AkkaHttpWebContext].getSessionId.asInstanceOf[Option[AnyRef]].asJava

  override def buildFromTrackableSession(context: WebContext, trackableSession: scala.Any): Optional[SessionStore] = {
    trackableSession match {
      case session: String if session.nonEmpty =>
        context.asInstanceOf[AkkaHttpWebContext].trackSession(session)
        Optional.of(this)

      case _ =>
        Optional.empty()
    }
  }

  override def renewSession(ctx: WebContext): Boolean = {
    val context = ctx.asInstanceOf[AkkaHttpWebContext]
    context.getSessionId.foreach { sessionId =>
      val sessionValues = context.sessionStorage.getSessionValues(sessionId)
      destroySession(context)
      val newSessionId = context.getOrCreateSessionId()
      sessionValues.foreach(context.sessionStorage.setSessionValues(newSessionId, _))
    }
    true
  }
}
