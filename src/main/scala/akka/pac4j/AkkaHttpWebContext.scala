package akka.pac4j

import org.pac4j.core.context.{Cookie, WebContext}

class AkkaHttpWebContext extends WebContext {

  override def getRequestCookies = ???

  override def addResponseCookie(cookie: Cookie) = ???

  override def getSessionStore = ???

  override def getRemoteAddr = ???

  override def setResponseHeader(name: String, value: String) = ???

  override def getRequestParameters = ???

  override def getFullRequestURL = ???

  override def getServerName = ???

  override def setResponseContentType(content: String) = ???

  override def writeResponseContent(content: String) = ???

  override def getPath = ???

  override def setResponseStatus(code: Int) = ???

  override def getRequestParameter(name: String) = ???

  override def getRequestHeader(name: String) = ???

  override def getScheme = ???

  override def isSecure = ???

  override def getRequestMethod = ???

  override def getServerPort = ???

  override def setRequestAttribute(name: String, value: scala.Any) = ???

  override def getRequestAttribute(name: String) = ???
}
