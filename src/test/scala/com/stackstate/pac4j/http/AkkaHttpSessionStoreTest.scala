package com.stackstate.pac4j.http

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.AkkaHttpWebContext
import com.stackstate.pac4j.store.{ForgetfulSessionStorage, InMemorySessionStorage}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Optional
import scala.concurrent.duration._

class AkkaHttpSessionStoreTest extends AnyWordSpecLike with Matchers with ScalatestRouteTest {
  "AkkaHttpSessionStore.getOrCreateSessionId" should {
    "return a valid session if one doesn't exist" in {
      val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      new AkkaHttpSessionStore().getOrCreateSessionId(context).matches(uuidRegex) shouldBe true
    }

    "return an existing session if one exist" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      context.trackSession("foo")
      new AkkaHttpSessionStore().getOrCreateSessionId(context) shouldBe "foo"
    }
  }

  "AkkaHttpSessionStore.get" should {
    "return null when the data is not available" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      new AkkaHttpSessionStore().get(context, "mykey") shouldBe Optional.empty()
    }

    "return the data when available" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      new AkkaHttpSessionStore().set(context, "mykey", "yooo")
      new AkkaHttpSessionStore().get(context, "mykey") shouldBe Optional.of("yooo")
    }
  }

  "AkkaHttpSessionStore.destroySession" should {
    "result in an empty session" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      val sessionStore = new AkkaHttpSessionStore()
      sessionStore.set(context, "mykey", "yooo")
      sessionStore.destroySession(context)
      sessionStore.get(context, "mykey") shouldBe Optional.empty()
    }
  }

  "AkkaHttpSessionStore.getTrackableSession" should {
    "return an optional of the current session" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      context.trackSession("foo")
      new AkkaHttpSessionStore().getTrackableSession(context) shouldBe Optional.of("foo")
    }

    "return empty when there is no current session" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      new AkkaHttpSessionStore().getTrackableSession(context) shouldBe Optional.empty()
    }
  }

  "AkkaHttpSessionStore.renewSession" should {
    "result in a non-empty session" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      val sessionStore = new AkkaHttpSessionStore()
      sessionStore.set(context, "mykey", "bar")
      sessionStore.renewSession(context)
      sessionStore.get(context, "mykey") should not be Optional.of("invalid")
      sessionStore.get(context, "mykey") shouldBe Optional.of("bar")
    }
  }
}
