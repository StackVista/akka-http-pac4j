package com.stackstate.pac4j.http

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.http.adapter.HttpActionAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.RouteResult.Complete
import com.stackstate.pac4j.AkkaHttpWebContext

import scala.concurrent.Future

object AkkaHttpActionAdapter extends HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext] {
  override def adapt(code: Int, context: AkkaHttpWebContext): Future[Complete] = {
    Future.successful(Complete(code match {
      case HttpConstants.UNAUTHORIZED =>
        HttpResponse(Unauthorized)
      case HttpConstants.BAD_REQUEST =>
        HttpResponse(BadRequest)
      case HttpConstants.CREATED =>
        HttpResponse(Created)
      case HttpConstants.FORBIDDEN =>
        HttpResponse(Forbidden)
      case HttpConstants.TEMP_REDIRECT =>
        context.addResponseSessionCookie()
        HttpResponse(SeeOther)
      case HttpConstants.OK =>
        val contentBytes = context.getResponseContent.getBytes
        val entity = context.getContentType.map(ct => HttpEntity(ct, contentBytes)).getOrElse(HttpEntity(contentBytes))
        HttpResponse(OK, entity = entity)
      case HttpConstants.NO_CONTENT =>
        HttpResponse(NoContent)
      case 500 =>
        HttpResponse(InternalServerError)
    }))
  }
}
