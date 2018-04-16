package com.stackstate.pac4j

import akka.http.scaladsl.model.HttpRequest
import org.pac4j.core.context.{ Cookie, WebContext }
import scala.collection.JavaConverters._

/**
 * The AkkaHttpWebContext is responsible for wrapping an HTTP request and stores changes that are produced by pac4j and
 * need to be applied to an HTTP response.
 */
case class AkkaHttpWebContext(request: HttpRequest) extends WebContext {
  import com.stackstate.pac4j.AkkaHttpWebContext._

  private var changes = ResponseChanges.empty

  override def getRequestCookies: java.util.Collection[Cookie] = {
    request.cookies.map { akkaCookie =>
      new Cookie(akkaCookie.name, akkaCookie.value)
    }.asJavaCollection
  }

  override def addResponseCookie(cookie: Cookie): Unit = {
    changes = changes.copy(cookies = changes.cookies ++ List(cookie))
  }

  override def getSessionStore = ???

  override def getRemoteAddr: String = {
    request.getUri().getHost.address()
  }

  override def setResponseHeader(name: String, value: String): Unit = {
    changes = changes.copy(headers = changes.headers ++ List((name, value)))
  }

  override def getRequestParameters: java.util.Map[String, Array[String]] = {
    request.getUri().query().toMap.asScala.mapValues(Array(_)).asJava
  }

  override def getFullRequestURL: String = {
    request.getUri().getScheme + "://" + request.getUri().getHost.address() + request.getUri().getPathString
  }

  override def getServerName: String = {
    request.getUri().host.address().split(":")(0)
  }

  override def setResponseContentType(contentType: String): Unit = {
    changes = changes.copy(contentType = contentType)
  }

  override def writeResponseContent(content: String): Unit = {
    if (content != null) {
      changes = changes.copy(content = changes.content + content)
    } else ()
  }

  override def getPath: String = {
    request.getUri().path
  }

  override def setResponseStatus(code: Int): Unit = ()

  override def getRequestParameter(name: String): String = {
    request.getUri().query().getOrElse(name, "")
  }

  override def getRequestHeader(name: String): String = {
    request.headers.find(_.name() == name).map(_.value).getOrElse("")
  }

  override def getScheme: String = {
    request.getUri().getScheme
  }

  override def isSecure: Boolean = {
    val scheme = request.getUri().getScheme.toLowerCase

    scheme == "https" || scheme == "ldap"
  }

  override def getRequestMethod: String = {
    request.method.value
  }

  override def getServerPort: Int = {
    request.getUri().getPort
  }

  override def setRequestAttribute(name: String, value: scala.AnyRef): Unit = {
    changes = changes.copy(attributes = changes.attributes ++ Map[String, AnyRef](name -> value))
  }

  override def getRequestAttribute(name: String): AnyRef = {
    changes.attributes.getOrElse(name, "")
  }

  def getChanges: ResponseChanges = changes
}

object AkkaHttpWebContext {

  //This class is where all the HTTP response changes are stored so that they can later be applied to an HTTP Request
  case class ResponseChanges(
    headers: List[(String, String)],
    contentType: String,
    content: String,
    cookies: List[Cookie],
    attributes: Map[String, AnyRef])

  object ResponseChanges {
    def empty: ResponseChanges = {
      ResponseChanges(List.empty, "", "", List.empty, Map.empty)
    }
  }

}
