package com.stackstate.pac4j

import org.pac4j.core.profile.CommonProfile

/**
 * Class which serves as a witness when authentication was successful. This class is private such that the user of this
 * library cannot mess with it.
 */
case class AuthenticatedRequest private[pac4j] (
  private[pac4j] val webContext: AkkaHttpWebContext,
  /**
   * Profiles can be accessed such that the user can inspect data from the authentication
   */
  val profiles: List[CommonProfile]) {
}
