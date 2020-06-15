package com.stackstate.pac4j

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.StatusCodes._
import akka.util.ByteString
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import com.stackstate.pac4j.store.ForgetfulSessionStorage
import org.scalatest.concurrent.ScalaFutures

class AkkaHttpActionAdapterTest extends AnyWordSpecLike with Matchers with ScalaFutures {
  "AkkaHttpActionAdapter" should {
    "convert 200 to OK" in withContext { context =>
      AkkaHttpActionAdapter.adapt(200, context).futureValue.response shouldEqual HttpResponse(OK, Nil, HttpEntity(ContentTypes.`application/octet-stream`, ByteString("")))
    }
    "convert 401 to Unauthorized" in withContext { context =>
      AkkaHttpActionAdapter.adapt(401, context).futureValue.response shouldEqual HttpResponse(Unauthorized)
    }
    "convert 302 to SeeOther (to support login flow)" in withContext { context =>
      AkkaHttpActionAdapter.adapt(302, context).futureValue.response shouldEqual HttpResponse(SeeOther)
    }
    "convert 400 to BadRequest" in withContext { context =>
      AkkaHttpActionAdapter.adapt(400, context).futureValue.response shouldEqual HttpResponse(BadRequest)
    }
    "convert 201 to Created" in withContext { context =>
      AkkaHttpActionAdapter.adapt(201, context).futureValue.response shouldEqual HttpResponse(Created)
    }
    "convert 403 to Forbidden" in withContext { context =>
      AkkaHttpActionAdapter.adapt(403, context).futureValue.response shouldEqual HttpResponse(Forbidden)
    }
    "convert 204 to NoContent" in withContext { context =>
      AkkaHttpActionAdapter.adapt(204, context).futureValue.response shouldEqual HttpResponse(NoContent)
    }
    "convert 200 to OK with content set from the context" in withContext { context =>
      context.writeResponseContent("content")
      AkkaHttpActionAdapter.adapt(200, context).futureValue.response shouldEqual HttpResponse.apply(OK, Nil, HttpEntity(ContentTypes.`application/octet-stream`, ByteString("content")))
    }
    "convert 200 to OK with content type set from the context" in withContext { context =>
      context.setResponseContentType("application/json")
      AkkaHttpActionAdapter.adapt(200, context).futureValue.response shouldEqual HttpResponse.apply(OK, Nil, HttpEntity(ContentTypes.`application/json`, ByteString("")))
    }
  }
  
  def withContext(f: AkkaHttpWebContext => Unit) = {
    f(AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME))
  }
}
