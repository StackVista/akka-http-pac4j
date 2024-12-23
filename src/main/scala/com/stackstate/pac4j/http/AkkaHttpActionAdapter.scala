package com.stackstate.pac4j.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RouteResult.Complete
import com.stackstate.pac4j.AkkaHttpWebContext
import org.pac4j.core.context.{HttpConstants, WebContext}
import org.pac4j.core.exception.http._
import org.pac4j.core.http.adapter.HttpActionAdapter

import scala.concurrent.Future

object AkkaHttpActionAdapter extends HttpActionAdapter {
  override def adapt(action: HttpAction, ctx: WebContext): Future[Complete] = {
    val context = ctx.asInstanceOf[AkkaHttpWebContext]
    Future.successful(Complete(action match {
      case _: UnauthorizedAction =>
        // XHR requests don't receive a TEMP_REDIRECT but a UNAUTHORIZED. The client can handle this
        // to trigger the proper redirect anyway, but for a correct flow the session cookie must be set
        context.addResponseSessionCookie()
        HttpResponse(Unauthorized)
      case _: BadRequestAction =>
        HttpResponse(BadRequest)
      case _ if action.getCode == HttpConstants.CREATED =>
        HttpResponse(Created)
      case _: ForbiddenAction =>
        HttpResponse(Forbidden)
      case a: FoundAction =>
        context.addResponseSessionCookie()
        HttpResponse(SeeOther, headers = List[HttpHeader](Location(Uri(a.getLocation))))
      case a: SeeOtherAction =>
        context.addResponseSessionCookie()
        HttpResponse(SeeOther, headers = List[HttpHeader](Location(Uri(a.getLocation))))
      case a: OkAction =>
        val contentBytes = a.getContent.getBytes
        val entity = context.getContentType.map(ct => HttpEntity(ct, contentBytes)).getOrElse(HttpEntity(contentBytes))
        HttpResponse(OK, entity = entity)
      case _: NoContentAction =>
        HttpResponse(NoContent)
      case _ if action.getCode == 500 =>
        HttpResponse(InternalServerError)
      case _ =>
        HttpResponse(StatusCodes.getForKey(action.getCode).getOrElse(custom(action.getCode, "")))
    }))
  }
}
