package com.stackstate.pac4j

import java.util

import akka.http.scaladsl.model.HttpRequest
import org.pac4j.core.context.{ Cookie, WebContext }

case class AkkaHttpWebContext(request: HttpRequest) extends WebContext {

  override def getRequestCookies: util.Collection[Cookie] = ???

  override def addResponseCookie(cookie: Cookie): Unit = ???

  override def getSessionStore = ???

  override def getRemoteAddr: String = ???

  override def setResponseHeader(name: String, value: String): Unit = ???

  override def getRequestParameters: util.Map[String, Array[String]] = ???

  override def getFullRequestURL: String = ???

  override def getServerName: String = ???

  override def setResponseContentType(content: String): Unit = ???

  override def writeResponseContent(content: String): Unit = ???

  override def getPath: String = ???

  override def setResponseStatus(code: Int): Unit = ???

  override def getRequestParameter(name: String): String = ???

  override def getRequestHeader(name: String): String = ???

  override def getScheme: String = ???

  override def isSecure: Boolean = ???

  override def getRequestMethod: String = ???

  override def getServerPort: Int = ???

  override def setRequestAttribute(name: String, value: scala.AnyRef): Unit = ???

  override def getRequestAttribute(name: String): AnyRef = ???
}
