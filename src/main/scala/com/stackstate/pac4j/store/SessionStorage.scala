package com.stackstate.pac4j.store

import scala.concurrent.duration.FiniteDuration

object SessionStorage {
  type SessionKey = String
  type ValueKey = String
}

trait SessionStorage {
  import SessionStorage._

  /**
    * Time after which a session gets automatically destroyed
    */
  val sessionLifetime: FiniteDuration

  /**
    * Ensure existance of a session with the provided key. Returns whether the session is new
    */
  def createSessionIfNeeded(sessionKey: SessionKey): Boolean

  /**
    * Check whether a session exists
    */
  def sessionExists(sessionKey: SessionKey): Boolean

  /**
    * Get value stored in existing session, returns None when either the session or value cannot be found
    */
  def getSessionValue(sessionKey: SessionKey, key: ValueKey): Option[AnyRef]

  /**
    * Get all values stored in an existing session, returns None when the session id or the values cannot be found
    */
  def getSessionValues(sessionKey: SessionKey): Option[Map[ValueKey, AnyRef]]

  /**
    * Set a value for a given session. Returns false if the session did not exist
    */
  def setSessionValue(sessionKey: SessionKey, key: ValueKey, value: scala.AnyRef): Boolean

  /**
    * Set all values for a given session. Returns false if the session did not exist
    */
  def setSessionValues(sessionKey: SessionKey, values: Map[ValueKey, AnyRef]): Boolean

  /**
    * Renew a session, meaning its lifetime start from 'now'
    */
  def renewSession(sessionKey: SessionKey): Boolean

  /**
    * Destroy a session
    */
  def destroySession(sessionKey: SessionKey): Boolean

}
