package com.stackstate.pac4j.store

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.store.InMemorySessionStorage.{DataRecord, ExpiryRecord}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.reflectiveCalls

class InMemorySessionStorageTest extends AnyWordSpecLike with Matchers with ScalatestRouteTest {

  def mockedTimeStorage(lifetime: FiniteDuration = 0.millis) = new InMemorySessionStorage(lifetime) {
    var time: Long = 1
    override private[store] def getTime = time
  }

  "InMemorySessionStorage.ensureSession" should {
    "add a data and expiry record" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session") shouldBe true
      storage.expiryQueue shouldBe Set(ExpiryRecord(1L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(1L, Map.empty))
    }

    "return false when the session already existed" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session") shouldBe true
      storage.createSessionIfNeeded("session") shouldBe false
    }

    "expire earlier sessions" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.expiryQueue shouldBe Set(ExpiryRecord(1L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(1L, Map.empty))
      storage.time = 2
      storage.createSessionIfNeeded("session2")
      storage.expiryQueue shouldBe Set(ExpiryRecord(2L, "session2"))
      storage.sessionData shouldBe Map("session2" -> DataRecord(2L, Map.empty))
    }
  }

  "InMemorySessionStorage.sessionExists" should {
    "return false when no session is there" in {
      val storage = mockedTimeStorage()
      storage.sessionExists("yoo") shouldBe false
    }

    "return true when a session is there" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.sessionExists("session") shouldBe true
    }

    "expire earlier sessions" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.expiryQueue shouldBe Set(ExpiryRecord(1L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(1L, Map.empty))
      storage.time = 2
      storage.sessionExists("session") shouldBe false
      storage.expiryQueue shouldBe Set()
      storage.sessionData shouldBe Map()
    }
  }

  "InMemorySessionStorage.getSessionValue" should {
    "return nothing when the session does not exist" in {
      val storage = mockedTimeStorage()
      storage.getSessionValue("yoo", "mykey") shouldBe None
    }

    "return nothing when the key session does not exist" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.getSessionValue("session", "mykey") shouldBe None
    }

    "return the set value when the key session does not exist" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.setSessionValue("session", "mykey", "yoo")
      storage.getSessionValue("session", "mykey") shouldBe Some("yoo")
    }

    "expire earlier sessions" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.expiryQueue shouldBe Set(ExpiryRecord(1L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(1L, Map.empty))
      storage.time = 2
      storage.getSessionValue("session", "mykey") shouldBe None
      storage.expiryQueue shouldBe Set()
      storage.sessionData shouldBe Map()
    }
  }

  "InMemorySessionStorage.getSessionValues" should {
    "return nothing when the session does not exist" in {
      mockedTimeStorage().getSessionValues("session") shouldEqual None
    }

    "return an empty map after the session is first created" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")

      storage.getSessionValues("session") shouldEqual Some(Map.empty)
    }

    "returned stored data" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")

      storage.setSessionValues("session", Map("abc" -> "def")) shouldEqual true
      storage.getSessionValues("session") shouldEqual Some(Map("abc" -> "def"))
    }
  }

  "InMemorySessionStorage.setSessionValue" should {
    "return false when the session does not exist" in {
      val storage = mockedTimeStorage()
      storage.setSessionValue("yoo", "mykey", "yoo") shouldBe false
    }

    "return true when setting succeeded" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.setSessionValue("session", "mykey", "yoo") shouldBe true
    }

    "overwrite a previous value when setting" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.setSessionValue("session", "mykey", "yoo") shouldBe true
      storage.setSessionValue("session", "mykey", "yoo2") shouldBe true
      storage.getSessionValue("session", "mykey") shouldBe Some("yoo2")
    }

    "expire earlier sessions" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.expiryQueue shouldBe Set(ExpiryRecord(1L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(1L, Map.empty))
      storage.time = 2
      storage.setSessionValue("session", "mykey", "yoo") shouldBe false
      storage.expiryQueue shouldBe Set()
      storage.sessionData shouldBe Map()
    }
  }

  "InMemorySessionStorage.setSessionValues" should {
    "return false when the session does not exist" in {
      mockedTimeStorage().setSessionValues("abc", Map.empty) shouldEqual false
    }

    "return true when setting succeeded" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.setSessionValues("session", Map("abc" -> "def")) shouldEqual true
    }

    "append values to existing ones, or overwrite them if the keys already exist" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.setSessionValues("session", Map("abc" -> "def")) shouldEqual true
      storage.getSessionValues("session") shouldEqual Some(Map("abc" -> "def"))

      storage.setSessionValues("session", Map("bla" -> "bla"))
      storage.getSessionValues("session") shouldEqual Some(Map("abc" -> "def", "bla" -> "bla"))

      storage.setSessionValues("session", Map("abc" -> "abc"))
      storage.getSessionValues("session") shouldEqual Some(Map("abc" -> "abc", "bla" -> "bla"))
    }
  }

  "InMemorySessionStorage.renewSession" should {
    "return false when the session does not exist" in {
      val storage = mockedTimeStorage()
      storage.renewSession("yoo") shouldBe false
    }

    "return true when setting succeeded and up the registered time" in {
      val storage = mockedTimeStorage(1.millis)
      storage.createSessionIfNeeded("session")
      storage.time = 2
      storage.renewSession("session") shouldBe true
      storage.expiryQueue shouldBe Set(ExpiryRecord(2L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(2L, Map.empty))
    }

    "expire earlier sessions" in {
      val storage = mockedTimeStorage(1.millis)
      storage.createSessionIfNeeded("session")
      storage.createSessionIfNeeded("session2")
      storage.time = 2
      storage.renewSession("session") shouldBe true
      storage.time = 3
      storage.renewSession("session") shouldBe true
      storage.expiryQueue shouldBe Set(ExpiryRecord(3L, "session"))
      storage.sessionData shouldBe Map("session" -> DataRecord(3L, Map.empty))
    }
  }

  "InMemorySessionStorage.destroySession" should {
    "return false when the session does not exist" in {
      val storage = mockedTimeStorage()
      storage.destroySession("yoo") shouldBe false
    }

    "return true when destroying succeeded and remove all data" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.destroySession("session") shouldBe true
      storage.expiryQueue shouldBe Set()
      storage.sessionData shouldBe Map()
    }

    "expire earlier sessions" in {
      val storage = mockedTimeStorage()
      storage.createSessionIfNeeded("session")
      storage.time = 2
      storage.destroySession("session2") shouldBe false
      storage.expiryQueue shouldBe Set()
      storage.sessionData shouldBe Map()
    }
  }
}
