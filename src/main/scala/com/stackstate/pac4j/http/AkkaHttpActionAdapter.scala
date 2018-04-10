package com.stackstate.pac4j.http

import akka.http.scaladsl.model.HttpResponse
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.http.adapter.HttpActionAdapter
import akka.http.scaladsl.model.StatusCodes._
import com.stackstate.pac4j.AkkaHttpWebContext

object AkkaHttpActionAdapter extends HttpActionAdapter[HttpResponse, AkkaHttpWebContext] {
  override def adapt(code: Int, context: AkkaHttpWebContext) = {
    code match {
      case HttpConstants.UNAUTHORIZED => HttpResponse(Unauthorized)
      case HttpConstants.BAD_REQUEST => HttpResponse(BadRequest)
      case HttpConstants.CREATED => HttpResponse(Created)
      case HttpConstants.FORBIDDEN => HttpResponse(Forbidden)
      case HttpConstants.OK => HttpResponse(OK)
      case HttpConstants.NO_CONTENT => HttpResponse(NoContent)
    }
  }
}
