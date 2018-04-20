package com.stackstate.pac4j.http

import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.session.SessionStore

/**
  * TODO: Implement an actual session store
  */
class DummySessionStore extends SessionStore[AkkaHttpWebContext] {
  override def getOrCreateSessionId(context: AkkaHttpWebContext): String = ???

  override def get(context: AkkaHttpWebContext, key: String): AnyRef = null

  override def set(context: AkkaHttpWebContext, key: String, value: scala.Any): Unit = ???

  override def destroySession(context: AkkaHttpWebContext): Boolean = ???

  override def getTrackableSession(context: AkkaHttpWebContext): AnyRef = ???

  override def buildFromTrackableSession(context: AkkaHttpWebContext, trackableSession: scala.Any): SessionStore[AkkaHttpWebContext] = ???

  override def renewSession(context: AkkaHttpWebContext): Boolean = ???
}
