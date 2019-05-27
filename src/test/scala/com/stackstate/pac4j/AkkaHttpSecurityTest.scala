package com.stackstate.pac4j

import java.{lang, util}

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, RequestContext, Route, RouteResult}
import akka.http.scaladsl.model._
import com.stackstate.pac4j.AkkaHttpSecurity.{AkkaHttpCallbackLogic, AkkaHttpLogoutLogic, AkkaHttpSecurityLogic}
import org.pac4j.core.config.Config
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.context.{Cookie, Pac4jConstants, WebContext}
import org.scalatest.{Matchers, WordSpecLike}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.AkkaHttpSecurity.{AkkaHttpCallbackLogic, AkkaHttpLogoutLogic, AkkaHttpSecurityLogic}
import com.stackstate.pac4j.http.AkkaHttpActionAdapter
import com.stackstate.pac4j.store.{ForgetfulSessionStorage, InMemorySessionStorage}
import org.pac4j.core.authorization.authorizer.Authorizer
import org.pac4j.core.client.{Clients, IndirectClient}
import org.pac4j.core.config.Config
import org.pac4j.core.context.{Cookie, Pac4jConstants, WebContext}
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.engine.{DefaultCallbackLogic, DefaultLogoutLogic, DefaultSecurityLogic, SecurityGrantedAccessAdapter}
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.profile.CommonProfile
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

class AkkaHttpSecurityTest extends WordSpecLike with Matchers with ScalatestRouteTest {

  "AkkaHttpSecurity" should {
    "set the proper defaults" in {
      val config = new Config()

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)
      akkaHttpSecurity.actionAdapter shouldBe AkkaHttpActionAdapter
      akkaHttpSecurity.securityLogic.getClass shouldBe classOf[DefaultSecurityLogic[_, _]]
      akkaHttpSecurity.callbackLogic.getClass shouldBe classOf[DefaultCallbackLogic[_, _]]
    }
  }

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
          authorizers shouldBe "myauthorizers" // Empty string means always authorize in DefaultAuthorizationCheck.java
          multiProfile shouldBe false

          httpActionAdapter shouldBe actionAdapter
          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withAuthentication("myclients", multiProfile = false, authorizers = "myauthorizers") { _ => complete("problem!") } ~> check {
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
            authenticated.profiles.head shouldBe profile
            complete("called!")
          }
        }

      Get("/") ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }
  }

  "AkkaHttpSecurity.withContext" should {
    "sets response headers when they are set in the context" in {
      val config = new Config()
      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withContext() { context =>
        context.setResponseHeader("MyHeader", "MyValue")
        complete("called!")
      } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
        header("MyHeader").get.value() shouldBe "MyValue"
      }
    }

    "sets response cookies (deduplicated) when they're set in the context" in {
      val config = new Config()
      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.withContext() { context =>
        val cookie = new Cookie("MyCookie", "MyValue")
        cookie.setSecure(true)
        cookie.setMaxAge(100)
        cookie.setHttpOnly(true)
        cookie.setPath("/")

        val cookie2 = new Cookie("MyCookie", "MyValue")
        cookie2.setSecure(true)
        cookie2.setMaxAge(100)
        cookie2.setHttpOnly(true)
        cookie2.setPath("/")

        context.addResponseCookie(cookie)
        context.addResponseCookie(cookie2)
        complete("called!")
      } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
        headers.size shouldBe 1
        header("Set-Cookie").get.value() shouldBe "MyCookie=MyValue; Max-Age=100; Path=/; Secure; HttpOnly"
      }
    }

    "get request parameters from a form" in {
      val config = new Config()
      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      val postRequest = HttpRequest(
        HttpMethods.POST,
        "/",
        entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, () => HttpCharsets.`UTF-8`), "username=testuser".getBytes)
      )

      postRequest ~> akkaHttpSecurity.withFormParameters(enforceFormEncoding = false) { params =>
        params("username") shouldEqual "testuser"
        complete("called!")
      } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "fail a request when no parameters exist in a form and enforceFormEncoding is enabled" in {
      val config = new Config()
      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      val postRequest = HttpRequest(
        HttpMethods.POST,
        "/",
        entity = HttpEntity(ContentType(MediaTypes.`application/json`), "".getBytes)
      )

      postRequest ~> akkaHttpSecurity.withFormParameters(enforceFormEncoding = true) { _ => fail("perform should never be called!") } ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

  "AkkaHttpSecurity.callback" should {
    "uses provided callbackLogic and pass the expected parameters" in {
      val config = new Config()

      val actionAdapter = new HttpActionAdapter[HttpResponse, AkkaHttpWebContext] {
        override def adapt(code: Int, context: AkkaHttpWebContext): HttpResponse = ???
      }

      config.setHttpActionAdapter(actionAdapter)
      config.setCallbackLogic(new AkkaHttpCallbackLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], defaultUrl: String, saveInSession: lang.Boolean, multiProfile: lang.Boolean, renewSession: lang.Boolean, client: String): Future[RouteResult] = {
          httpActionAdapter shouldBe actionAdapter
          defaultUrl shouldBe "/blaat"
          saveInSession shouldBe false
          multiProfile shouldBe false
          renewSession shouldBe true
          client shouldBe "Yooo"

          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.callback("/blaat", saveInSession = false, multiProfile = false, Some("Yooo")) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "run the callbackLogic reusing an akka http context" in {
      val config = new Config()
      val existingContext = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)

      val actionAdapter = new HttpActionAdapter[HttpResponse, AkkaHttpWebContext] {
        override def adapt(code: Int, context: AkkaHttpWebContext): HttpResponse = ???
      }

      config.setHttpActionAdapter(actionAdapter)
      config.setCallbackLogic(new AkkaHttpCallbackLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], defaultUrl: String, saveInSession: lang.Boolean, multiProfile: lang.Boolean, renewSession: lang.Boolean, client: String): Future[RouteResult] = {
          existingContext.sessionId shouldBe context.sessionId
          httpActionAdapter shouldBe actionAdapter
          defaultUrl shouldBe "/blaat"
          saveInSession shouldBe false
          multiProfile shouldBe false
          renewSession shouldBe true
          client shouldBe "Yooo"

          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.callback("/blaat", saveInSession = false, multiProfile = false, Some("Yooo"), existingContext = Some(existingContext)) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }
  }

  "AkkaHttpSecurity.authorize" should {
    "pass the provided authenticationRequest to the authorizer" in {
      val profile = new CommonProfile()
      val context = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)

      val authorizer = new Authorizer[CommonProfile] {
        override def isAuthorized(context: WebContext, profiles: util.List[CommonProfile]): Boolean = {
          profiles.size() shouldBe 1
          profiles.get(0) shouldBe profile
          false
        }
      }
      val route: Route =
        AkkaHttpSecurity.authorize(authorizer)(AuthenticatedRequest(context, List(profile))) {
          complete("oops!")
        }

      (Get("/") ~> route).rejections shouldBe Seq(AuthorizationFailedRejection)
    }

    "reject when authorization fails" in {
      val context = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)

      val authorizer = new Authorizer[CommonProfile] {
        override def isAuthorized(context: WebContext, profiles: util.List[CommonProfile]): Boolean = false
      }
      val route =
        AkkaHttpSecurity.authorize(authorizer)(AuthenticatedRequest(context, List.empty)) {
          complete("oops!")
        }

      (Get("/") ~> route).rejections shouldBe Seq(AuthorizationFailedRejection)
    }

    "succeed when authorization succeeded" in {
      val context = AkkaHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, AkkaHttpWebContext.DEFAULT_COOKIE_NAME)

      val authorizer = new Authorizer[CommonProfile] {
        override def isAuthorized(context: WebContext, profiles: util.List[CommonProfile]): Boolean = true
      }
      val route =
        AkkaHttpSecurity.authorize(authorizer)(AuthenticatedRequest(context, List.empty)) {
          complete("cool!")
        }

      Get("/") ~> route ~> check {
        status shouldBe StatusCodes.OK
        entityAs[String] shouldBe "cool!"
      }
    }
  }

  "AkkaHttpSecurity.logout" should {
    "run the callbackLogic with the expected parameters" in {
      val config = new Config()

      config.setHttpActionAdapter(AkkaHttpActionAdapter)
      config.setLogoutLogic(new AkkaHttpLogoutLogic {
        override def perform(context: AkkaHttpWebContext, config: Config, httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], defaultUrl: String, logoutUrlPattern: String, localLogout: lang.Boolean, destroySession: lang.Boolean, centralLogout: lang.Boolean): Future[RouteResult] = {
          httpActionAdapter shouldBe AkkaHttpActionAdapter
          defaultUrl shouldBe "/home"
          logoutUrlPattern shouldBe "*"
          localLogout shouldBe false
          destroySession shouldBe false

          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "logout!")))
        }
      })

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> akkaHttpSecurity.logout("/home", "*", localLogout = false, destroySession = false) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "logout!"
      }
    }

    "destroy the session and create a new empty one" in {
      val config = new Config()

      val client = new IndirectClient[UsernamePasswordCredentials, CommonProfile] {
        override def clientInit(): Unit = ???
      }

      val logoutLogic = new DefaultLogoutLogic[Future[RouteResult], AkkaHttpWebContext] {
        override def perform(context: AkkaHttpWebContext, config: Config, httpActionAdapter: HttpActionAdapter[Future[RouteResult], AkkaHttpWebContext], defaultUrl: String, inputLogoutUrlPattern: String, inputLocalLogout: lang.Boolean, inputDestroySession: lang.Boolean, inputCentralLogout: lang.Boolean): Future[RouteResult] = {

          context.sessionStorage.setSessionValue(context.sessionId, Pac4jConstants.USER_PROFILES, "Profile")
          context.sessionStorage.getSessionValue(context.sessionId, Pac4jConstants.USER_PROFILES) contains "Profile"

          val response = super.perform(context, config, httpActionAdapter, defaultUrl, inputLogoutUrlPattern, inputLocalLogout, inputDestroySession, inputCentralLogout)
          context.sessionStorage.getSessionValue(context.sessionId, Pac4jConstants.USER_PROFILES) shouldBe empty

          response
        }
      }

      config.setHttpActionAdapter(AkkaHttpActionAdapter)
      config.setLogoutLogic(logoutLogic)
      config.setClients(new Clients("url", client))

      val akkaHttpSecurity = new AkkaHttpSecurity(config, new InMemorySessionStorage(5000.seconds))

      Get("/") ~> akkaHttpSecurity.logout("/home", "*") ~> check {
        status shouldEqual StatusCodes.SeeOther
        header("Location").get.value shouldBe "/home"
      }
    }
  }
}
