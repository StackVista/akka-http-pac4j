package com.stackstate.pac4j.http

import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.session.SessionStore

class AkkaHttpSessionStore() extends SessionStore[AkkaHttpWebContext] {
  override def getOrCreateSessionId(context: AkkaHttpWebContext): String = context.sessionId

  override def get(context: AkkaHttpWebContext, key: String): Object =
    context.sessionStorage.getSessionValue(context.sessionId, key).orNull

  override def set(context: AkkaHttpWebContext, key: String, value: scala.AnyRef): Unit =
    context.sessionStorage.setSessionValue(context.sessionId, key, value)

  override def destroySession(context: AkkaHttpWebContext): Boolean = context.destroySession()

  override def getTrackableSession(context: AkkaHttpWebContext): AnyRef = context.sessionId

  override def buildFromTrackableSession(context: AkkaHttpWebContext, trackableSession: scala.Any): SessionStore[AkkaHttpWebContext] = {
    context.trackSession(trackableSession.asInstanceOf[String])
    this
  }

  override def renewSession(context: AkkaHttpWebContext): Boolean = {
    val sessionId = getOrCreateSessionId(context)
    val sessionValues = context.sessionStorage.getSessionValues(sessionId)
    destroySession(context)

    val newSessionId = getOrCreateSessionId(context)
    sessionValues.foreach (context.sessionStorage.setSessionValues(newSessionId, _))

    true
  }
}
