package com.stackstate.pac4j

import java.{lang, util}

import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model._
import com.stackstate.pac4j.AkkaHttpSecurity.AkkaHttpSecurityLogic
import org.pac4j.core.config.Config
import org.pac4j.core.engine.{DefaultSecurityLogic, SecurityGrantedAccessAdapter}
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.context.Cookie
import org.scalatest.{Matchers, WordSpecLike}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import com.stackstate.pac4j.store.ForgetfulSessionStorage
import org.pac4j.core.context.WebContext
import org.pac4j.core.profile.CommonProfile

import scala.collection.JavaConverters._
import scala.concurrent.Future

class AkkaHttpSecurityTest extends WordSpecLike with Matchers with ScalatestRouteTest {

  "AkkaHttpSecurity.withAuthentication" should {
    "uses provided securityLogic and pass the expected parameters" in {
      val config = new Config()

      val actionAdapter = new HttpActionAdapter[HttpResponse, AkkaHttpWebContext] {
        override def adapt(code: Int, context: AkkaHttpWebContext): HttpResponse = ???
      }

      config.setHttpActionAdapter(actionAdapter)
      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          clients shouldBe "myclients"
          matchers shouldBe "" // Empty string means always matching hit in RequireAllMatchersChecker.java
          authorizers shouldBe "" // Empty string means always authorize in DefaultAuthorizationCheck.java
          multiProfile shouldBe true

          httpActionAdapter shouldBe actionAdapter
          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withAuthentication("myclients", true) { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "calls inner route with authenticated profiles from directive when securityGrantedAccessAdapter is invoked and produces output" in {
      val config = new Config()
      val profile = new CommonProfile()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          securityGrantedAccessAdapter.adapt(context, List(profile).asJava)
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)
      val route =
        akkaHttpSecurity.withAuthentication() { authenticated =>
          {
            authenticated.profiles.size shouldBe 1
            authenticated.profiles(0) shouldBe profile
            complete("called!")
          }
        }

      Get("/") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "sets response headers when they are set in the security logic" in {
      val config = new Config()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          context.setResponseHeader("MyHeader", "MyValue")
          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withAuthentication() { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
        header("MyHeader").get.value() shouldBe "MyValue"
      }
    }

    "sets the proper defaults" in {
      val config = new Config()

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)
      akkaHttpSecurity.actionAdapter shouldBe AkkaHttpActionAdapter
      akkaHttpSecurity.securityLogic.getClass shouldBe classOf[DefaultSecurityLogic[_, _]]
    }

    "sets response cookies when they're set in the security logic" in {
      val config = new Config()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          context.addResponseCookie(new Cookie("MyCookie", "MyValue"))
          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withAuthentication() { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
        header("Set-Cookie").get.value() shouldBe "MyCookie=MyValue"
      }
    }

    "sets response content type when its set in the security logic" in {
      val config = new Config()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          context.setResponseContentType("text/html; charset=UTF-8")
          httpActionAdapter.adapt(200, context)
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withAuthentication() { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe ""
        contentType shouldEqual ContentTypes.`text/html(UTF-8)`
      }
    }

    "sets response content when its written by the security logic" in {
      val config = new Config()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          context.writeResponseContent("called!")
          httpActionAdapter.adapt(200, context)
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withAuthentication() { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "get request parameters from a form" in {
      val config = new Config()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          context.getRequestParameter("username") shouldEqual "testuser"
          httpActionAdapter.adapt(200, context)
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      val postRequest = HttpRequest(
        HttpMethods.POST,
        "/",
        entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), "username=testuser".getBytes)
      )

      postRequest ~> akkaHttpSecurity.withAuthentication(enforceFormEncoding = true) { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "fail a request when no parameters exist in a form and enforceFormEncoding is enabled" in {
      val config = new Config()

      config.setSecurityLogic(new AkkaHttpSecurityLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, securityGrantedAccessAdapter: SecurityGrantedAccessAdapter[Future[RouteResult], AkkaHttpWebContext], httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], clients: String, authorizers: String, matchers: String, multiProfile: lang.Boolean, parameters: AnyRef*): Future[RouteResult] = {
          fail("perform should never be called!")
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      val postRequest = HttpRequest(
        HttpMethods.POST,
        "/",
        entity = HttpEntity(ContentType(MediaTypes.`application/json`), "".getBytes)
      )

      postRequest ~> akkaHttpSecurity.withAuthentication(enforceFormEncoding = true) { _ => complete("problem!") } ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

  "AkkaHttpSecurity.authorize" should {
    "pass the provided authenticationRequest to the authorizer" in {
      val profile = new CommonProfile()
      val context = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage)

      val route =
        AkkaHttpSecurity.authorize((context: WebContext, profiles: util.List[CommonProfile]) => {
          profiles.size() shouldBe 1
          profiles.get(0) shouldBe profile
          false
        })(AuthenticatedRequest(context, List(profile))) {
          complete("oops!")
        }

      (Get("/") ~> route).rejections shouldBe Seq(AuthorizationFailedRejection)
    }

    "reject when authorization fails" in {
      val context = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage)

      val route =
        AkkaHttpSecurity.authorize((context: WebContext, profiles: util.List[CommonProfile]) => {
          false
        })(AuthenticatedRequest(context, List.empty)) {
          complete("oops!")
        }

      (Get("/") ~> route).rejections shouldBe Seq(AuthorizationFailedRejection)
    }

    "succeed when authorization succeeded" in {
      val context = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage)

      val route =
        AkkaHttpSecurity.authorize((context: WebContext, profiles: util.List[CommonProfile]) => {
          true
        })(AuthenticatedRequest(context, List.empty)) {
          complete("cool!")
        }

      Get("/") ~> route ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "cool!"
      }
    }
  }
}
