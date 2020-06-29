package com.stackstate.pac4j

import org.pac4j.core.profile.UserProfile

/**
  * Class which serves as a witness when authentication was successful. This class is private such that the user of this
  * library cannot mess with it.
  */
case class AuthenticatedRequest private[pac4j] (private[pac4j] val webContext: AkkaHttpWebContext,
                                                /*
                                                 * Profiles can be accessed such that the user can inspect data from the authentication. Should not be empty after
                                                 * authentication
                                                 */
                                                val profiles: List[UserProfile]) {

  lazy val mainProfile: UserProfile = profiles.head
}
