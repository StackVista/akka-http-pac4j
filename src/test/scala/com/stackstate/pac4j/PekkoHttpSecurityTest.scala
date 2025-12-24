package com.stackstate.pac4j

import java.{lang, util}

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.{HttpCookie, `Set-Cookie`}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{AuthorizationFailedRejection}
import org.apache.pekko.http.scaladsl.server.RouteResult.Complete
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import com.stackstate.pac4j.PekkoHttpSecurity.{PekkoHttpCallbackLogic, PekkoHttpLogoutLogic, PekkoHttpSecurityLogic}
import com.stackstate.pac4j.http.PekkoHttpActionAdapter
import com.stackstate.pac4j.store.{ForgetfulSessionStorage, InMemorySessionStorage}
import org.pac4j.core.client.{Clients, IndirectClient}
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.context.{Cookie, WebContext}
import org.pac4j.core.engine.{DefaultCallbackLogic, DefaultLogoutLogic, DefaultSecurityLogic, SecurityGrantedAccessAdapter}
import org.pac4j.core.exception.http.HttpAction
import org.pac4j.core.http.adapter.HttpActionAdapter
import org.pac4j.core.matching.matcher.DefaultMatchers
import org.pac4j.core.profile.{CommonProfile, UserProfile}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import org.pac4j.core.util.Pac4jConstants

class PekkoHttpSecurityTest extends AnyWordSpecLike with Matchers with ScalatestRouteTest {

  "PekkoHttpSecurity" should {
    "set the proper defaults" in {
      val config = new Config()

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)
      pekkoHttpSecurity.actionAdapter shouldBe PekkoHttpActionAdapter
      pekkoHttpSecurity.securityLogic.getClass shouldBe classOf[DefaultSecurityLogic]
      pekkoHttpSecurity.callbackLogic.getClass shouldBe classOf[DefaultCallbackLogic]
    }
  }

  "PekkoHttpSecurity.withAuthentication" should {
    "uses provided securityLogic and pass the expected parameters" in {
      val config = new Config()

      val actionAdapter = new HttpActionAdapter {
        override def adapt(code: HttpAction, context: WebContext): HttpResponse = ???
      }

      config.setHttpActionAdapter(actionAdapter)
      val securityLogic: PekkoHttpSecurityLogic = (_: WebContext,
                                                   _: SessionStore,
                                                   _: Config,
                                                   _: SecurityGrantedAccessAdapter,
                                                   httpActionAdapter: HttpActionAdapter,
                                                   clients: String,
                                                   authorizers: String,
                                                   matchers: String,
                                                   _: AnyRef) => {
        clients shouldBe "myclients"
        matchers shouldBe DefaultMatchers.SECURITYHEADERS
        matchers should not be empty
        authorizers shouldBe "myauthorizers" // Empty string means always authorize in DefaultAuthorizationCheck.java

        httpActionAdapter shouldBe actionAdapter
        Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
      }
      config.setSecurityLogic(securityLogic)

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity.withAuthentication("myclients", authorizers = "myauthorizers") { _ =>
        complete("problem!")
      } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "calls inner route with authenticated profiles from directive when securityGrantedAccessAdapter is invoked and produces output" in {
      val config = new Config()
      val profile = new CommonProfile()

      val securityLogic: PekkoHttpSecurityLogic = (context: WebContext,
                                                   sessionStore: SessionStore,
                                                   _: Config,
                                                   securityGrantedAccessAdapter: SecurityGrantedAccessAdapter,
                                                   _: HttpActionAdapter,
                                                   _: String,
                                                   _: String,
                                                   _: String,
                                                   _: AnyRef) => {
        securityGrantedAccessAdapter.adapt(context, sessionStore, List[UserProfile](profile).asJava)
      }
      config.setSecurityLogic(securityLogic)
      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)
      val route =
        pekkoHttpSecurity.withAuthentication() { authenticated =>
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

  "PekkoHttpSecurity.withContext" should {
    "sets response headers when they are set in the context" in {
      val config = new Config()
      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity.withContext() { context =>
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
      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity.withContext() { context =>
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
        headers.size shouldBe 2
        header("Set-Cookie").get.value() shouldBe "MyCookie=MyValue; Max-Age=100; Path=/; Secure; HttpOnly"
      }
    }

    "get request parameters from a form" in {
      val config = new Config()
      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      val postRequest =
        HttpRequest(HttpMethods.POST, "/", entity = HttpEntity(MediaTypes.`application/x-www-form-urlencoded`, "username=testuser".getBytes))

      postRequest ~> pekkoHttpSecurity.withFormParameters(enforceFormEncoding = false) { params =>
        params("username") shouldEqual "testuser"
        complete("called!")
      } ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "fail a request when no parameters exist in a form and enforceFormEncoding is enabled" in {
      val config = new Config()
      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      val postRequest = HttpRequest(HttpMethods.POST, "/", entity = HttpEntity(ContentType(MediaTypes.`application/json`), "".getBytes))

      postRequest ~> pekkoHttpSecurity.withFormParameters(enforceFormEncoding = true) { _ =>
        fail("perform should never be called!")
      } ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

  "PekkoHttpSecurity.callback" should {
    "uses provided callbackLogic and pass the expected parameters" in {
      val config = new Config()

      val actionAdapter = new HttpActionAdapter {
        override def adapt(action: HttpAction, context: WebContext): AnyRef = ???
      }

      config.setHttpActionAdapter(actionAdapter)
      config.setCallbackLogic(new PekkoHttpCallbackLogic {
        override def perform(webContext: WebContext,
                             sessionStore: SessionStore,
                             config: Config,
                             httpActionAdapter: HttpActionAdapter,
                             defaultUrl: String,
                             renewSession: lang.Boolean,
                             defaultClient: String): AnyRef = {
          httpActionAdapter shouldBe actionAdapter
          defaultUrl shouldBe "/blaat"
          renewSession shouldBe true
          defaultClient shouldBe "Yooo"

          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))

        }
      })

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity.callback("/blaat", Some("Yooo")) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "run the callbackLogic reusing an pekko http context" in {
      val config = new Config()
      val existingContext = PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)

      val actionAdapter = new HttpActionAdapter {
        override def adapt(action: HttpAction, context: WebContext): AnyRef = ???
      }

      config.setHttpActionAdapter(actionAdapter)
      config.setCallbackLogic(new PekkoHttpCallbackLogic {
        override def perform(webContext: WebContext,
                             sessionStore: SessionStore,
                             config: Config,
                             httpActionAdapter: HttpActionAdapter,
                             defaultUrl: String,
                             renewSession: lang.Boolean,
                             defaultClient: String): AnyRef = {
          existingContext.getSessionId shouldBe webContext.asInstanceOf[PekkoHttpWebContext].getSessionId
          httpActionAdapter shouldBe actionAdapter
          defaultUrl shouldBe "/blaat"
          renewSession shouldBe true
          defaultClient shouldBe "Yooo"

          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity
        .callback("/blaat", Some("Yooo"), existingContext = Some(existingContext)) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "called!"
      }
    }

    "run the callbackLogic should not send back a sessionId if the set csrf cookie is false" in {
      val config = new Config()
      val existingContext = PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)

      val actionAdapter = new HttpActionAdapter {
        override def adapt(action: HttpAction, context: WebContext): AnyRef = ???
      }
      config.setHttpActionAdapter(actionAdapter)
      config.setCallbackLogic(new PekkoHttpCallbackLogic {
        override def perform(webContext: WebContext,
                             sessionStore: SessionStore,
                             config: Config,
                             httpActionAdapter: HttpActionAdapter,
                             defaultUrl: String,
                             renewSession: lang.Boolean,
                             defaultClient: String): AnyRef = {
          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity
        .callback("/blaat", Some("Yooo"), existingContext = Some(existingContext), setCsrfCookie = false) ~> check {
        // Session Store is empty so `addResponseSessionCookie` will create a token that will expire immediately
        header("Set-Cookie").get.value().contains("PekkoHttpPac4jSession=; Max-Age=0;") shouldBe true

      }
    }

    "run the callbackLogic should send back a sessionId if the csrf cookie is true" in {
      val config = new Config()
      val existingContext = PekkoHttpWebContext(
        HttpRequest(uri = "http://test.com"),
        Seq.empty,
        new InMemorySessionStorage(3.minutes),
        PekkoHttpWebContext.DEFAULT_COOKIE_NAME
      )

      val actionAdapter = new HttpActionAdapter {
        override def adapt(action: HttpAction, context: WebContext): AnyRef = ???
      }
      config.setHttpActionAdapter(actionAdapter)
      config.setCallbackLogic(new PekkoHttpCallbackLogic {
        override def perform(webContext: WebContext,
                             sessionStore: SessionStore,
                             config: Config,
                             httpActionAdapter: HttpActionAdapter,
                             defaultUrl: String,
                             renewSession: lang.Boolean,
                             defaultClient: String): AnyRef = {
          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "called!")))
        }
      })

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new InMemorySessionStorage(3.minutes))
      Get("http://test.com/") ~> pekkoHttpSecurity
        .callback("/blaat", Some("Yooo"), existingContext = Some(existingContext), setCsrfCookie = true) ~> check {
        val localHeaders: Seq[HttpHeader] = headers
        val threeMinutesInSeconds = 180
        // When `addResponseCsrfCookie` is called the method `getOrCreateSessionId` is called which creates a Session
        // when `addResponseSessionCookie` is called there is already a session so a cookie with value is added.
        localHeaders.find(_.value().contains("pac4jCsrfToken")).get.value().contains(s"Max-Age=$threeMinutesInSeconds;") shouldBe true
        localHeaders.find(_.value().contains("PekkoHttpPac4jSession")).get.value().contains(s"Max-Age=$threeMinutesInSeconds;") shouldBe true

        val csrfCookies: Seq[HttpCookie] = localHeaders.collect {
          case setCookie: `Set-Cookie` if setCookie.cookie.name == "pac4jCsrfToken" => setCookie.cookie
        }
        // Previous version always added the two cookies. Current version doesn't need domain.
        // We add the two to keep it backwards compatible.
        csrfCookies.filter(_.domain.nonEmpty).size shouldBe 1
        csrfCookies.filter(_.domain.isEmpty).size shouldBe 1
      }
    }
  }

  "PekkoHttpSecurity.authorize" should {
    "pass the provided authenticationRequest to the authorizer" in {
      val profile = new CommonProfile()
      val context = PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)

      val route =
        PekkoHttpSecurity.authorize((_: WebContext, _: SessionStore, profiles: util.List[UserProfile]) => {
          profiles.size() shouldBe 1
          profiles.get(0) shouldBe profile
          false
        })(AuthenticatedRequest(context, List(profile))) {
          complete("oops!")
        }

      (Get("/") ~> route).rejections shouldBe Seq(AuthorizationFailedRejection)
    }

    "reject when authorization fails" in {
      val context = PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)

      val route =
        PekkoHttpSecurity.authorize((_: WebContext, _: SessionStore, _: util.List[UserProfile]) => {
          false
        })(AuthenticatedRequest(context, List.empty)) {
          complete("oops!")
        }

      (Get("/") ~> route).rejections shouldBe Seq(AuthorizationFailedRejection)
    }

    "succeed when authorization succeeded" in {
      val context = PekkoHttpWebContext(HttpRequest(), Seq.empty, new ForgetfulSessionStorage, PekkoHttpWebContext.DEFAULT_COOKIE_NAME)

      val route =
        PekkoHttpSecurity.authorize((_: WebContext, _: SessionStore, _: util.List[UserProfile]) => {
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

  "PekkoHttpSecurity.logout" should {
    "run the callbackLogic with the expected parameters" in {
      val config = new Config()

      config.setHttpActionAdapter(PekkoHttpActionAdapter)
      config.setLogoutLogic(new PekkoHttpLogoutLogic {
        override def perform(context: WebContext,
                             sessionStore: SessionStore,
                             config: Config,
                             httpActionAdapter: HttpActionAdapter,
                             defaultUrl: String,
                             logoutUrlPattern: String,
                             localLogout: lang.Boolean,
                             destroySession: lang.Boolean,
                             centralLogout: lang.Boolean): AnyRef = {
          httpActionAdapter shouldBe PekkoHttpActionAdapter
          defaultUrl shouldBe "/home"
          logoutUrlPattern shouldBe "*"
          localLogout shouldBe false
          destroySession shouldBe false

          Future.successful(Complete(HttpResponse(StatusCodes.OK, entity = "logout!")))
        }
      })

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new ForgetfulSessionStorage)

      Get("/") ~> pekkoHttpSecurity.logout("/home", "*", localLogout = false, destroySession = false) ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldBe "logout!"
      }
    }

    "destroy the session and create a new empty one" in {
      val config = new Config()

      val client = new IndirectClient {
        override def internalInit(forceReinit: Boolean): Unit = ???
      }

      val logoutLogic = new DefaultLogoutLogic {
        override def perform(ctx: WebContext,
                             sessionStore: SessionStore,
                             config: Config,
                             httpActionAdapter: HttpActionAdapter,
                             defaultUrl: String,
                             inputLogoutUrlPattern: String,
                             inputLocalLogout: lang.Boolean,
                             inputDestroySession: lang.Boolean,
                             inputCentralLogout: lang.Boolean): AnyRef = {
          val context = ctx.asInstanceOf[PekkoHttpWebContext]
          val profiles = new util.HashMap[String, UserProfile]()
          profiles.put("john", new CommonProfile())
          context.sessionStorage.setSessionValue(context.getOrCreateSessionId(), Pac4jConstants.USER_PROFILES, profiles)
          context.sessionStorage.getSessionValue(context.getOrCreateSessionId(), Pac4jConstants.USER_PROFILES) contains profiles

          val response = super
            .perform(
              context,
              sessionStore,
              config,
              httpActionAdapter,
              defaultUrl,
              inputLogoutUrlPattern,
              inputLocalLogout,
              inputDestroySession,
              inputCentralLogout
            )
          context.sessionStorage.getSessionValue(context.getOrCreateSessionId(), Pac4jConstants.USER_PROFILES) shouldBe empty

          response
        }
      }

      config.setHttpActionAdapter(PekkoHttpActionAdapter)
      config.setLogoutLogic(logoutLogic)
      config.setClients(new Clients("url", client))

      val pekkoHttpSecurity = new PekkoHttpSecurity(config, new InMemorySessionStorage(5000.seconds))

      Get("/") ~> pekkoHttpSecurity.logout("/home", "*") ~> check {
        status shouldEqual StatusCodes.SeeOther
        header("Location").get.value shouldBe "/home"
      }
    }
  }
}
