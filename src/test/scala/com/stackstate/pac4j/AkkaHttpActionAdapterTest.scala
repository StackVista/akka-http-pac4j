package com.stackstate.pac4j

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.util.ByteString
import com.stackstate.pac4j.AkkaHttpActionAdapterTest.ActionInt
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import com.stackstate.pac4j.store.ForgetfulSessionStorage
import org.pac4j.core.exception.http.{
  BadRequestAction,
  ForbiddenAction,
  FoundAction,
  HttpAction,
  NoContentAction,
  OkAction,
  StatusAction,
  UnauthorizedAction
}
import org.scalatest.concurrent.ScalaFutures

class AkkaHttpActionAdapterTest extends AnyWordSpecLike with Matchers with ScalaFutures {

  lazy val immediatelyExpireCookie: HttpCookie =
    HttpCookie.apply(name = AkkaHttpWebContext.DEFAULT_COOKIE_NAME, value = "", maxAge = Some(0), path = Some("/"), httpOnly = true)

  "AkkaHttpActionAdapter" should {
    "convert 200 to OK" in withContext { context =>
      AkkaHttpActionAdapter.adapt(new OkAction(""), context).futureValue.response shouldEqual HttpResponse(
        OK,
        Nil,
        HttpEntity(ContentTypes.`application/octet-stream`, ByteString(""))
      )
    }
    "convert 401 to Unauthorized for direct clients" in withContext { context =>
      AkkaHttpActionAdapter.adapt(new UnauthorizedAction, context).futureValue.response shouldEqual HttpResponse(Unauthorized)
      context.getChanges.cookies shouldBe List(immediatelyExpireCookie)
    }
    "convert 401 to Unauthorized for indirect clients" in withContext { context =>
      context.getSessionStore.set(context, "state", "foo")
      AkkaHttpActionAdapter.adapt(new UnauthorizedAction, context).futureValue.response shouldEqual HttpResponse(Unauthorized)
      context.getChanges.cookies.map(_.name) shouldBe List(AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
    }
    "convert 302 to SeeOther (to support login flow) (direct client)" in withContext { context =>
      val r = AkkaHttpActionAdapter.adapt(new FoundAction("/login"), context).futureValue.response
      r.status shouldEqual SeeOther
      r.headers.head.value() shouldEqual "/login"
      context.getChanges.cookies shouldBe List(immediatelyExpireCookie)
    }
    "convert 302 to SeeOther (to support login flow) (indirect client)" in withContext { context =>
      context.getSessionStore.set(context, "state", "foo")
      val r = AkkaHttpActionAdapter.adapt(new FoundAction("/login"), context).futureValue.response
      r.status shouldEqual SeeOther
      r.headers.head.value() shouldEqual "/login"
      context.getChanges.cookies.map(_.name) shouldBe List(AkkaHttpWebContext.DEFAULT_COOKIE_NAME)
    }
    "convert 400 to BadRequest" in withContext { context =>
      AkkaHttpActionAdapter.adapt(new BadRequestAction, context).futureValue.response shouldEqual HttpResponse(BadRequest)
    }
    "convert 201 to Created" in withContext { context =>
      AkkaHttpActionAdapter.adapt(201.action(), context).futureValue.response shouldEqual HttpResponse(Created)
    }
    "convert 403 to Forbidden" in withContext { context =>
      AkkaHttpActionAdapter.adapt(new ForbiddenAction, context).futureValue.response shouldEqual HttpResponse(Forbidden)
    }
    "convert 204 to NoContent" in withContext { context =>
      AkkaHttpActionAdapter.adapt(NoContentAction.INSTANCE, context).futureValue.response shouldEqual HttpResponse(NoContent)
    }
    "convert 200 to OK with content set from the context" in withContext { context =>
      AkkaHttpActionAdapter.adapt(new OkAction("content"), context).futureValue.response shouldEqual HttpResponse
        .apply(OK, Nil, HttpEntity(ContentTypes.`application/octet-stream`, ByteString("content")))
    }
    "convert 200 to OK with content type set from the context" in withContext { context =>
      context.setResponseContentType("application/json")
      AkkaHttpActionAdapter.adapt(new OkAction(""), context).futureValue.response shouldEqual HttpResponse
        .apply(OK, Nil, HttpEntity(ContentTypes.`application/json`, ByteString("")))
    }
  }

  def withContext(f: AkkaHttpWebContext => Unit): Unit = {
    f(AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME))
  }
}

object AkkaHttpActionAdapterTest {
  implicit class ActionInt(val i: Int) extends AnyVal {
    def action(): HttpAction = new StatusAction(i)
  }
}
