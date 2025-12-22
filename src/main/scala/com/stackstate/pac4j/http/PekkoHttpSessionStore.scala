package com.stackstate.pac4j.http

import java.util.Optional
import scala.jdk.OptionConverters._
import com.stackstate.pac4j.PekkoHttpWebContext
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore

class PekkoHttpSessionStore() extends SessionStore {
  override def getSessionId(context: WebContext, createSession: Boolean): Optional[String] = {
    if (createSession) {
      Optional.of(context.asInstanceOf[PekkoHttpWebContext].getOrCreateSessionId())
    } else {
      context.asInstanceOf[PekkoHttpWebContext].getSessionId.toJava
    }

  }

  override def get(context: WebContext, key: String): Optional[Object] =
    context.asInstanceOf[PekkoHttpWebContext].getSessionId match {
      case Some(value) => context.asInstanceOf[PekkoHttpWebContext].sessionStorage.getSessionValue(value, key).toJava
      case None => Optional.empty()
    }

  override def set(context: WebContext, key: String, value: scala.AnyRef): Unit = {
    context
      .asInstanceOf[PekkoHttpWebContext]
      .sessionStorage
      .setSessionValue(context.asInstanceOf[PekkoHttpWebContext].getOrCreateSessionId(), key, value)
    ()
  }

  override def destroySession(context: WebContext): Boolean = context.asInstanceOf[PekkoHttpWebContext].destroySession()

  override def getTrackableSession(context: WebContext): Optional[AnyRef] =
    context.asInstanceOf[PekkoHttpWebContext].getSessionId.asInstanceOf[Option[AnyRef]].toJava

  override def buildFromTrackableSession(context: WebContext, trackableSession: scala.Any): Optional[SessionStore] = {
    trackableSession match {
      case session: String if session.nonEmpty =>
        context.asInstanceOf[PekkoHttpWebContext].trackSession(session)
        Optional.of(this)

      case _ =>
        Optional.empty()
    }
  }

  override def renewSession(ctx: WebContext): Boolean = {
    val context = ctx.asInstanceOf[PekkoHttpWebContext]
    context.getSessionId.foreach { sessionId =>
      val sessionValues = context.sessionStorage.getSessionValues(sessionId)
      destroySession(context)
      val newSessionId = context.getOrCreateSessionId()
      sessionValues.foreach(context.sessionStorage.setSessionValues(newSessionId, _))
    }
    true
  }
}
