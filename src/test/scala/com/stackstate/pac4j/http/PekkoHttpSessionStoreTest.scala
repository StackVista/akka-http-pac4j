package com.stackstate.pac4j.http

import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.PekkoHttpWebContext
import com.stackstate.pac4j.store.{ForgetfulSessionStorage, InMemorySessionStorage}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Optional
import scala.concurrent.duration._

class PekkoHttpSessionStoreTest extends AnyWordSpecLike with Matchers with ScalatestRouteTest {
  "PekkoHttpSessionStore.get" should {
    "return the None if nothing exists in the store" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      new PekkoHttpSessionStore().getSessionId(context, createSession = false) shouldBe Optional.empty()
    }

    "return an existing session if one exist" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      context.trackSession("foo")
      new PekkoHttpSessionStore().getSessionId(context, createSession = false) shouldBe Optional.of("foo")
    }
  }

  "PekkoHttpSessionStore.getOrCreateSessionId" should {
    "return a valid session if one doesn't exist" in {
      val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      new PekkoHttpSessionStore().getSessionId(context, createSession = true).get().matches(uuidRegex) shouldBe true
    }

    "return an existing session if one exist" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      context.trackSession("foo")
      new PekkoHttpSessionStore().getSessionId(context, createSession = false).get() shouldBe "foo"
    }
  }

  "PekkoHttpSessionStore.get" should {
    "return null when the data is not available" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      new PekkoHttpSessionStore().get(context, "mykey") shouldBe Optional.empty()
    }

    "return the data when available" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      new PekkoHttpSessionStore().set(context, "mykey", "yooo")
      new PekkoHttpSessionStore().get(context, "mykey") shouldBe Optional.of("yooo")
    }
  }

  "PekkoHttpSessionStore.destroySession" should {
    "result in an empty session" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      val sessionStore = new PekkoHttpSessionStore()
      sessionStore.set(context, "mykey", "yooo")
      sessionStore.destroySession(context)
      sessionStore.get(context, "mykey") shouldBe Optional.empty()
    }
  }

  "PekkoHttpSessionStore.getTrackableSession" should {
    "return an optional of the current session" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      context.trackSession("foo")
      new PekkoHttpSessionStore().getTrackableSession(context) shouldBe Optional.of("foo")
    }

    "return empty when there is no current session" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      new PekkoHttpSessionStore().getTrackableSession(context) shouldBe Optional.empty()
    }
  }

  "PekkoHttpSessionStore.renewSession" should {
    "result in a non-empty session if a session exists" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      val sessionStore = new PekkoHttpSessionStore()
      sessionStore.set(context, "mykey", "bar")
      sessionStore.renewSession(context)
      sessionStore.get(context, "mykey") should not be Optional.of("invalid")
      sessionStore.get(context, "mykey") shouldBe Optional.of("bar")
    }

    "result in a empty session if a session doesn't exists" in {
      val context = new PekkoHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), PekkoHttpWebContext.DEFAULT_COOKIE_NAME)
      val sessionStore = new PekkoHttpSessionStore()
      sessionStore.renewSession(context)
      sessionStore.get(context, "mykey") shouldBe Optional.empty()
    }
  }
}
