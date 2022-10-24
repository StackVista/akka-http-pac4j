package com.stackstate.pac4j.store

import com.stackstate.pac4j.store.SessionStorage._

import scala.collection.immutable.SortedSet
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
  private[store] var sessionData = Map.empty[SessionKey, DataRecord]

  private[store] def getTime = System.currentTimeMillis()

  private def expireOldSessions(): Unit = {
    val expireFrom = getTime - sessionLifetimeMs

    val (expired, nonExpired) = expiryQueue.span(_.registered < expireFrom)
    sessionData = sessionData -- expired.map(_.id)
    expiryQueue = nonExpired
  }

  override def createSessionIfNeeded(sessionKey: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey) match {
        case None =>
          val sessionTime = getTime
          expiryQueue = expiryQueue + ExpiryRecord(sessionTime, sessionKey)
          sessionData = sessionData + (sessionKey -> DataRecord(sessionTime, Map.empty))
          true
        case Some(_) => false
      }
    }
  }

  override def sessionExists(sessionKey: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.contains(sessionKey)
    }
  }

  override def getSessionValue(sessionKey: SessionKey, key: ValueKey): Option[AnyRef] = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey).flatMap(_.data.get(key))
    }
  }

  override def getSessionValues(sessionKey: SessionKey): Option[Map[ValueKey, AnyRef]] = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey).map(_.data)
    }
  }

  override def setSessionValue(sessionKey: SessionKey, key: ValueKey, value: AnyRef): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey) match {
        case None => false
        case Some(DataRecord(registered, data)) =>
          sessionData = sessionData + (sessionKey -> DataRecord(registered, data + (key -> value)))
          true
      }
    }
  }

  override def setSessionValues(sessionKey: SessionKey, values: Map[ValueKey, AnyRef]): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey) match {
        case None =>
          false
        case Some(DataRecord(registered, data)) =>
          sessionData = sessionData + (sessionKey -> DataRecord(registered, data ++ values))
          true
      }
    }
  }

  override def destroySession(sessionKey: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey) match {
        case None => false
        case Some(data) =>
          expiryQueue = expiryQueue - ExpiryRecord(data.registered, sessionKey)
          sessionData = sessionData - sessionKey
          true
      }
    }
  }

  override def renewSession(sessionKey: SessionKey): Boolean = {
    this.synchronized {
      expireOldSessions()
      sessionData.get(sessionKey) match {
        case None => false
        case Some(DataRecord(registered, data)) =>
          val now = getTime
          sessionData = sessionData + (sessionKey -> DataRecord(now, data))
          expiryQueue = (expiryQueue - ExpiryRecord(registered, sessionKey)) + ExpiryRecord(now, sessionKey)
          true
      }
    }
  }
}
