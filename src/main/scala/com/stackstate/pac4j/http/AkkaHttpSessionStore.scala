package com.stackstate.pac4j.http

import java.util.Optional
import compat.java8.OptionConverters._
import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.session.SessionStore

class AkkaHttpSessionStore() extends SessionStore[AkkaHttpWebContext] {
  def getSessionId(context: AkkaHttpWebContext): Option[String] = context.getSessionId

  override def getOrCreateSessionId(context: AkkaHttpWebContext): String = context.getOrCreateSessionId()

  override def get(context: AkkaHttpWebContext, key: String): Optional[Object] =
    context.getSessionId match {
      case Some(value) => context.sessionStorage.getSessionValue(value, key).asJava
      case None => Optional.empty()
    }

  override def set(context: AkkaHttpWebContext, key: String, value: scala.AnyRef): Unit = {
    context.sessionStorage.setSessionValue(context.getOrCreateSessionId(), key, value)
    ()
  }

  override def destroySession(context: AkkaHttpWebContext): Boolean = context.destroySession()

  override def getTrackableSession(context: AkkaHttpWebContext): Optional[AnyRef] =
    context.getSessionId.asInstanceOf[Option[AnyRef]].asJava

  override def buildFromTrackableSession(context: AkkaHttpWebContext, trackableSession: scala.Any): Optional[SessionStore[AkkaHttpWebContext]] = {
    trackableSession match {
      case session: String if session.nonEmpty =>
        context.trackSession(session)
        Optional.of(this)

      case _ =>
        Optional.empty()
    }
  }

  override def renewSession(context: AkkaHttpWebContext): Boolean = {
    getSessionId(context).foreach { sessionId =>
      val sessionValues = context.sessionStorage.getSessionValues(sessionId)
      destroySession(context)
      val newSessionId = getOrCreateSessionId(context)
      sessionValues.foreach(context.sessionStorage.setSessionValues(newSessionId, _))
    }
    true
  }
}
