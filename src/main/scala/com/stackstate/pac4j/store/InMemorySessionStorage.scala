package com.stackstate.pac4j.store

import com.stackstate.pac4j.store.SessionStorage._

import scala.collection.SortedSet
import scala.concurrent.duration.FiniteDuration

object InMemorySessionStorage {
  private[store] type RegisteredMs = Long
  private[store] case class ExpiryRecord(registered: RegisteredMs, id: SessionKey)
  private[store] case class DataRecord(registered: RegisteredMs, data: Map[ValueKey, AnyRef])
}

class InMemorySessionStorage(override val sessionLifetime: FiniteDuration) extends SessionStorage {

  import InMemorySessionStorage._

  private val sessionLifetimeMs = sessionLifetime.toMillis

  private[store] var expiryQueue = SortedSet[ExpiryRecord]()(Ordering.by(v => (v.registered, v.id)))
  private[store] var sessionData = Map[SessionKey, DataRecord]()

  private[store] def getTime = System.currentTimeMillis()

  private def expireOldSessions(): Unit = {
    val expireFrom = getTime - sessionLifetimeMs

    val (expired, nonExpired) = expiryQueue.span(_.registered < expireFrom)
    sessionData = sessionData -- expired.map(_.id)
    expiryQueue = nonExpired
  }

  override def ensureSession(session: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(session) match {
        case None =>
          val sessionTime = getTime
          expiryQueue = expiryQueue + ExpiryRecord (sessionTime, session)
          sessionData = sessionData + (session -> DataRecord(sessionTime, Map.empty) )
          true
        case Some(_) => false
      }
    }
  }

  override def sessionExists(key: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.contains(key)
    }
  }

  override def getSessionValue(session: SessionKey, key: ValueKey): Option[AnyRef] = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(session).flatMap(_.data.get(key))
    }
  }

  override def setSessionValue(session: SessionKey, key: ValueKey, value: AnyRef): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(session) match {
        case None => false
        case Some(DataRecord(registered, data)) =>
          sessionData = sessionData + (session -> DataRecord(registered, data + (key -> value)))
          true
      }
    }
  }

  override def destroySession(session: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(session) match {
        case None => false
        case Some(data) =>
          expiryQueue = expiryQueue - ExpiryRecord(data.registered, session)
          sessionData = sessionData - session
          true
      }
    }
  }

  override def renewSession(session: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(session) match {
        case None => false
        case Some(DataRecord(registered, data)) =>
          val now = getTime
          sessionData = sessionData + (session -> DataRecord(now, data))
          expiryQueue = (expiryQueue - ExpiryRecord(registered, session)) + ExpiryRecord(now, session)
          true
      }
    }
  }
}
