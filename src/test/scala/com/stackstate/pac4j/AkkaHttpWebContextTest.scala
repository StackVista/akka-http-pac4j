package com.stackstate.pac4j

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest, Uri }
import org.pac4j.core.context.{ Cookie => PCookie }
import org.scalatest.{ Matchers, WordSpecLike }

class AkkaHttpWebContextTest extends WordSpecLike with Matchers {
  lazy val cookie = new PCookie("cookieName", "cookieValue")

  "AkkaHttpWebContext" should {
    "be able to get/set request cookies" in withContext(cookies = List(Cookie("cookieName", "cookieValue"))) { webContext =>
      val contextCookie = webContext.getRequestCookies
      contextCookie shouldEqual Array(cookie)
    }

    "be able to get a request header or an empty string when no such header exists" in withContext(requestHeaders = List(("foo", "bar"))) { webContext =>
      webContext.getRequestHeader("foo") shouldEqual "bar"
      webContext.getRequestHeader("abc") shouldEqual ""
    }

    "be able to get the full url from a request" in withContext(url = "www.stackstate.com/views") { webContext =>
      webContext.getFullRequestURL shouldEqual "http://www.stackstate.com/views"
    }

    "be able to get the request path" in withContext(url = "www.stackstate.com/views") { webContext =>
      webContext.getPath shouldEqual "views"
    }

    "be able to get the remote address from a request" in withContext(url = "www.stackstate.com/views", hostAddress = "localhost:8000") { webContext =>
      webContext.getRemoteAddr shouldEqual "localhost:8000"
    }

    "be able to get the request http method" in withContext() { webContext =>
      webContext.getRequestMethod shouldEqual "GET"
    }

    "be able to get server name and port from the request" in withContext(hostAddress = "localhost:8000") { webContext =>
      webContext.getServerName shouldEqual "localhost"
      webContext.getServerPort shouldEqual "8000"
    }

    "be able to get the uri scheme" in withContext() { webContext =>
      webContext.getScheme shouldEqual "http"
    }

    "be able to get the request parameters or an empty string when no such parameter exists" in withContext(url = "www.stackstate.com/views?v=1234") { webContext =>
      webContext.getRequestParameter("v") shouldEqual "1"
      webContext.getRequestParameter("p") shouldEqual ""
    }

    "be able to get/set request attributes" in withContext() { webContext =>
      webContext.setRequestAttribute("foo", "bar")
      webContext.getRequestAttribute("foo") shouldEqual "bar"
    }

    "be able to get/set response content" in withContext() { webContext =>
      webContext.writeResponseContent("content")
      webContext.getRequestContent shouldEqual "content"
    }
  }

  def withContext(
    requestHeaders: List[(String, String)] = List.empty,
    cookies: List[Cookie] = List.empty,
    url: String = "",
    scheme: String = "http",
    hostAddress: String = "")(f: AkkaHttpWebContext => Unit): Unit = {
    val parsedHeaders: List[HttpHeader] = requestHeaders.map { case (k, v) => HttpHeader.parse(k, v) }.collect { case Ok(header, _) => header }
    val completeHeaders: List[HttpHeader] = parsedHeaders ++ cookies
    val uri = Uri(url).withScheme(scheme).withHost(hostAddress)

    val request = HttpRequest(uri = uri, headers = completeHeaders)

    f(AkkaHttpWebContext(request))
  }
}
