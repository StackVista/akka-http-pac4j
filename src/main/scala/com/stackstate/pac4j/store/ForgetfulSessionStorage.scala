package com.stackstate.pac4j.store

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.stackstate.pac4j.store.SessionStorage._
import scala.concurrent.duration.FiniteDuration

class ForgetfulSessionStorage extends SessionStorage {

  override val sessionLifetime: FiniteDuration = FiniteDuration(0, TimeUnit.SECONDS)

  override def ensureSession(session: SessionKey): Boolean = true

  override def sessionExists(key: SessionKey): Boolean = false

  override def getSessionValue(session: SessionKey, key: ValueKey): Option[AnyRef] = None

  override def setSessionValue(session: SessionKey, key: ValueKey, value: AnyRef): Boolean = false

  override def destroySession(session: SessionKey): Boolean = false

  override def renewSession(session: SessionKey): Boolean = false
}
