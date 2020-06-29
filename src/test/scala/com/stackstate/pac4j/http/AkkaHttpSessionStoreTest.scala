package com.stackstate.pac4j.http

import java.util.Optional

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.AkkaHttpWebContext
import com.stackstate.pac4j.store.{ForgetfulSessionStorage, InMemorySessionStorage}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

class AkkaHttpSessionStoreTest extends WordSpecLike with Matchers with ScalatestRouteTest {
  "AkkaHttpSessionStore.get" should {
    "return null when the data is not available" in {
      new AkkaHttpSessionStore().get(
        new AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME),
        "mykey"
      ) shouldBe Optional.empty()
    }

    "return the data when available" in {
      val context = new AkkaHttpWebContext(HttpRequest(), Seq.empty, new InMemorySessionStorage(30.minutes), AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
      new AkkaHttpSessionStore().set(context, "mykey", "yooo")
      new AkkaHttpSessionStore().get(context, "mykey") shouldBe Optional.of("yooo")
    }
  }
}
