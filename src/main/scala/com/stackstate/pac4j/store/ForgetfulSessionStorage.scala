package com.stackstate.pac4j.store

import java.util.concurrent.TimeUnit

import com.stackstate.pac4j.store.SessionStorage._
import scala.concurrent.duration.FiniteDuration

class ForgetfulSessionStorage extends SessionStorage {

  override val sessionLifetime: FiniteDuration = FiniteDuration(0, TimeUnit.SECONDS)

  override def createSessionIfNeeded(sessionKey: SessionKey): Boolean = true

  override def sessionExists(sessionKey: SessionKey): Boolean = false

  override def getSessionValue(sessionKey: SessionKey, key: ValueKey): Option[AnyRef] = None

  override def setSessionValue(sessionKey: SessionKey, key: ValueKey, value: AnyRef): Boolean = false

  override def destroySession(sessionKey: SessionKey): Boolean = false

  override def renewSession(sessionKey: SessionKey): Boolean = false
}
