/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customerprofile.connector

import com.typesafe.config.Config
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UnhealthyServiceException}
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.customerprofile.domain.{EmailPreference, Paperless, Preference, TermsAccepted}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class EntityResolverConnectorSpec extends UnitSpec with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val hc = new HeaderCarrier

  private def defaultGetHandler: (String) => Future[AnyRef with HttpResponse] = {
    _ => Future.successful(HttpResponse(200))
  }

  private def defaultPostHandler: (String, Any) => Future[AnyRef with HttpResponse] = {
    (a, b) => Future.successful(HttpResponse(200))
  }

  private def defaultPutHandler: (String, Any) => Future[AnyRef with HttpResponse] = {
    (a, b) => Future.successful(HttpResponse(200))
  }

  class TestPreferencesConnector extends EntityResolverConnector with ServicesConfig with ServicesCircuitBreaker {
    val serviceUrl = "http://entity-resolver.service/"

    override protected def circuitBreakerConfig = CircuitBreakerConfig(externalServiceName, 5, 2000, 2000)

    def http: CoreGet with CorePost = ???
  }

  def entityResolverConnector(returnFromDoGet: String => Future[HttpResponse] = defaultGetHandler,
                              returnFromDoPost: (String, Any) => Future[HttpResponse] = defaultPostHandler,
                              returnFromDoPut: (String, Any) => Future[HttpResponse] = defaultPutHandler) = new TestPreferencesConnector {
    override val http = new CoreGet with HttpGet with CorePost with HttpPost with AppName {
      def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = returnFromDoGet(url)

      def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = returnFromDoPost(url, body)

      def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = returnFromDoPut(url, body)

      val hooks: Seq[HttpHook] = NoneRequired

      override def configuration: Option[Config] = None
    }
  }



  "The getPreferences method" should {
    val nino = Nino("CE123457D")

    "return the preferences for utr only" in {
      val preferenceConnector = entityResolverConnector { url =>
        url should not include nino.value

        Future.successful(HttpResponse(200, Some(Json.parse(
          """
            |{
            |   "digital": true,
            |   "email": {
            |     "email": "test@mail.com",
            |     "status": "verified",
            |     "mailboxFull": false
            |   }
            |}
          """.stripMargin))))
      }

      val preferences = preferenceConnector.getPreferences().futureValue

      preferences shouldBe Some(Preference(
        digital = true, email = Some(EmailPreference(
          email = EmailAddress("test@mail.com"),
          status = Status.Verified))
      ))
    }

    "return None for a 404" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(404, None)))

      val preferences = preferenceConnector.getPreferences().futureValue

      preferences shouldBe None
    }

    "return None for a 410" in {
      val preferenceConnector = entityResolverConnector(_ => Future.successful(HttpResponse(410, None)))

      val preferences = preferenceConnector.getPreferences().futureValue

      preferences shouldBe None
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {
      val connector = entityResolverConnector(
        returnFromDoGet = _ => Future.failed(new InternalServerException("some exception"))
      )

      1 to 5 foreach { _ =>
        connector.getPreferences().failed.futureValue shouldBe an[InternalServerException]
      }
      connector.getPreferences().failed.futureValue shouldBe an[UnhealthyServiceException]
    }
  }

  "The upgradeTermsAndConditions method" should {
    trait PayloadCheck {
      def status: Int = 200
      def expectedPayload: Paperless
      def postedPayload(payload: Paperless) = payload should be (expectedPayload)
      val email = EmailAddress("test@test.com")

      val connector = entityResolverConnector(returnFromDoPost = checkPayloadAndReturn)

      def checkPayloadAndReturn(url: String, requestBody: Any): Future[HttpResponse] = {
        postedPayload(requestBody.asInstanceOf[Paperless])
        Future.successful(HttpResponse(status))
      }
    }

    "send accepted true and return preferences created if terms and conditions are accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = Paperless(TermsAccepted(true), email)

      connector.paperlessSettings(Paperless(TermsAccepted(true), email)).futureValue should be (PreferencesExists)
    }

    "send accepted false and return preferences created if terms and conditions are not accepted and updated and preferences created" in new PayloadCheck {
      override val expectedPayload = Paperless(TermsAccepted(false), email)

      connector.paperlessSettings(Paperless(TermsAccepted(false), email)).futureValue should be (PreferencesExists)
    }

    "return failure if any problems" in new PayloadCheck {
      override val status = 401
      override val expectedPayload = Paperless(TermsAccepted(true), email)

      whenReady(connector.paperlessSettings(Paperless(TermsAccepted(true), email)).failed) {
        case e => e shouldBe an[Upstream4xxResponse]
      }
    }
  }

  "New user" should {
    trait NewUserPayloadCheck {
      def status: Int = 201
      def expectedPayload: Paperless
      def postedPayload(payload: Paperless) = payload should be (expectedPayload)
      val email = EmailAddress("test@test.com")

      val connector = entityResolverConnector(returnFromDoPost = checkPayloadAndReturn)

      def checkPayloadAndReturn(url: String, requestBody: Any): Future[HttpResponse] = {
        postedPayload(requestBody.asInstanceOf[Paperless])
        Future.successful(HttpResponse(status))
      }
    }

    "send accepted true with email" in new NewUserPayloadCheck {
      override def expectedPayload = Paperless(TermsAccepted(true), email)

      connector.paperlessSettings(Paperless(TermsAccepted(true), email)).futureValue should be (PreferencesCreated)
    }

    "try and send accepted true with email where preferences not working" in new NewUserPayloadCheck {
      override def expectedPayload = Paperless(TermsAccepted(true), email)

      override def status: Int = 401

      whenReady(connector.paperlessSettings(Paperless(TermsAccepted(true), email)).failed) {
        case e => e shouldBe an[Upstream4xxResponse]
      }
    }
  }

}
