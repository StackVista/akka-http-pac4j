package com.stackstate.pac4j.http

import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.http.adapter.HttpActionAdapter
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.RouteResult.Complete
import com.stackstate.pac4j.AkkaHttpWebContext

import scala.concurrent.Future

object AkkaHttpActionAdapter extends HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext] {
  override def adapt(code: Int, context: AkkaHttpWebContext) = {
    Future.successful(Complete(code match {
      case HttpConstants.UNAUTHORIZED => HttpResponse(Unauthorized)
      case HttpConstants.BAD_REQUEST => HttpResponse(BadRequest)
      case HttpConstants.CREATED => HttpResponse(Created)
      case HttpConstants.FORBIDDEN => HttpResponse(Forbidden)
      case HttpConstants.OK => HttpResponse(OK, entity = HttpEntity.apply(context.getContentType, context.getResponseContent.getBytes))
      case HttpConstants.NO_CONTENT => HttpResponse(NoContent)
      case 500 => HttpResponse(InternalServerError)
    }))
  }
}
