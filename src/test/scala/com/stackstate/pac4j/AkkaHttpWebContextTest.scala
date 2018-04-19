package com.stackstate.pac4j

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{ ContentTypes, HttpHeader, HttpRequest, Uri }
import org.scalatest.{ Matchers, WordSpecLike }

import scala.collection.JavaConverters._

class AkkaHttpWebContextTest extends WordSpecLike with Matchers {
  lazy val cookie = ("cookieName", "cookieValue")

  "AkkaHttpWebContext" should {
    "get/set request cookies" in withContext(cookies = List(Cookie("cookieName", "cookieValue"))) { webContext =>
      val contextCookie = webContext.getRequestCookies
      contextCookie.asScala.map(cookie => (cookie.getName, cookie.getValue)) shouldEqual Seq(cookie)
    }

    "get a request header or an empty string when no such header exists" in withContext(requestHeaders = List(("foo", "bar"))) { webContext =>
      webContext.getRequestHeader("foo") shouldEqual "bar"
      webContext.getRequestHeader("abc") shouldEqual ""
    }
    "make sure request headers are case insensitive" in withContext(requestHeaders = List(("foo", "bar"))) { webContext =>
      webContext.getRequestHeader("FOO") shouldEqual "bar"
    }

    "get the full url from a request" in withContext(url = "www.stackstate.com/views") { webContext =>
      webContext.getFullRequestURL shouldEqual "http://www.stackstate.com/views"
    }

    "get the request path" in withContext(url = "www.stackstate.com/views") { webContext =>
      webContext.getPath shouldEqual "www.stackstate.com/views"
    }

    "get the remote address from a request" in withContext(url = "/views", hostAddress = "192.1.1.1", hostPort = 8000) { webContext =>
      webContext.getRemoteAddr shouldEqual "192.1.1.1"
    }

    "get the request http method" in withContext() { webContext =>
      webContext.getRequestMethod shouldEqual "GET"
    }

    "get server name and port from the request" in withContext(hostAddress = "192.1.1.1", hostPort = 8000) { webContext =>
      webContext.getServerName shouldEqual "192.1.1.1"
      webContext.getServerPort shouldEqual 8000
    }

    "get the uri scheme" in withContext() { webContext =>
      webContext.getScheme shouldEqual "http"
    }

    "get the request parameters or an empty string when no such parameter exists" in withContext(url = "www.stackstate.com/views?v=1234") { webContext =>
      webContext.getRequestParameter("v") shouldEqual "1234"
      webContext.getRequestParameter("p") shouldEqual ""
    }

    "get/set request attributes" in withContext() { webContext =>
      webContext.setRequestAttribute("foo", "bar")
      webContext.getRequestAttribute("foo") shouldEqual "bar"
    }

    "get/set response content" in withContext() { webContext =>
      webContext.writeResponseContent("content")
      webContext.getResponseContent shouldEqual "content"
    }

    "get/set content type" in withContext() { webContext =>
      webContext.setResponseContentType("application/json")
      webContext.getContentType shouldEqual Some(ContentTypes.`application/json`)
    }

    "know if a url is secure" in withContext(scheme = "https") { webContext =>
      webContext.isSecure shouldEqual true
    }

    "return form fields in the request parameters" in withContext(formFields = Seq(("username", "testuser"))) { webContext =>
      webContext.getRequestParameters.containsKey("username") shouldEqual true
      webContext.getRequestParameter("username") shouldEqual "testuser"
    }
  }

  def withContext(
    requestHeaders: List[(String, String)] = List.empty,
    cookies: List[Cookie] = List.empty,
    url: String = "",
    scheme: String = "http",
    hostAddress: String = "",
    hostPort: Int = 0,
    formFields: Seq[(String, String)] = Seq.empty)(f: AkkaHttpWebContext => Unit): Unit = {
    val parsedHeaders: List[HttpHeader] = requestHeaders.map { case (k, v) => HttpHeader.parse(k, v) }.collect { case Ok(header, _) => header }
    val completeHeaders: List[HttpHeader] = parsedHeaders ++ cookies
    val uri = Uri(url).withScheme(scheme).withAuthority(hostAddress, hostPort)
    val request = HttpRequest(uri = uri, headers = completeHeaders)

    f(AkkaHttpWebContext(request, formFields))
  }
}
