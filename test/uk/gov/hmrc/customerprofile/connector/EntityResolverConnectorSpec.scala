/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.Writes
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{ConfigLoader, Configuration, Environment}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customerprofile.config.WSHttpImpl
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.{Pending, Verified}
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{NotFoundException, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EntityResolverConnectorSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with ScalaFutures
    with MockFactory {
  implicit val hc: HeaderCarrier = new HeaderCarrier

  val http:                                            WSHttpImpl    = mock[WSHttpImpl]
  val config:                                          Configuration = mock[Configuration]
  val environment:                                     Environment   = mock[Environment]
  val baseUrl:                                         String        = "http://entity-resolver.service"
  val termsAndCondtionssPostUrl:                       String        = s"$baseUrl/preferences/terms-and-conditions"
  val circuitBreakerNumberOfCallsToTriggerStateChange: Int           = 5

  // create a new connectopr each time because the circuit breaker is stateful
  def preferenceConnector: EntityResolverConnector = {
    def mockCircuitBreakerConfig() = {
      (config
        .getOptional[Configuration](_: String)(_: ConfigLoader[Configuration]))
        .expects("microservice.services.entity-resolver", *)
        .returns(Some(config))
        .anyNumberOfTimes()
      (config
        .getOptional[Int](_: String)(_: ConfigLoader[Int]))
        .expects("circuitBreaker.numberOfCallsToTriggerStateChange", *)
        .returns(Some(circuitBreakerNumberOfCallsToTriggerStateChange))
        .anyNumberOfTimes()
      (config
        .getOptional[Int](_: String)(_: ConfigLoader[Int]))
        .expects("circuitBreaker.unavailablePeriodDurationInSeconds", *)
        .returns(Some(2000))
        .anyNumberOfTimes()
      (config
        .getOptional[Int](_: String)(_: ConfigLoader[Int]))
        .expects("circuitBreaker.unstablePeriodDurationInSeconds", *)
        .returns(Some(2000))
        .anyNumberOfTimes()
    }

    mockCircuitBreakerConfig()
    new EntityResolverConnector(baseUrl, http, config, environment)
  }

  "getPreferences()" should {
    def mockHttpGET(preferences: Future[Option[Preference]]) =
      (http
        .GET(_: String)(_: HttpReads[Option[Preference]], _: HeaderCarrier, _: ExecutionContext))
        .expects(s"$baseUrl/preferences", *, *, *)
        .returns(preferences)

    "return the preferences for utr only" in {
      val preferences = Some(Preference(digital = true, Some(EmailPreference(EmailAddress("test@mail.com"), Verified))))

      mockHttpGET(Future successful preferences)

      await(preferenceConnector.getPreferences()) shouldBe preferences
    }

    "return the preferences with linkSent daye when email is pending" in {
      val preferences = Some(Preference(digital = true, Some(EmailPreference(EmailAddress("test@mail.com"), Pending, Some(LocalDate.now())))))

      mockHttpGET(Future successful preferences)

      await(preferenceConnector.getPreferences()) shouldBe preferences
    }

    "return None for a 404" in {
      mockHttpGET(Future failed new NotFoundException("where are you?"))

      await(preferenceConnector.getPreferences()) shouldBe None
    }

    "return None for a 410" in {
      mockHttpGET(Future failed Upstream4xxResponse("GONE", 410, 410))

      await(preferenceConnector.getPreferences()) shouldBe None
    }

    "circuit breaker configuration should be applied and unhealthy service exception will kick in after 5th failed call to preferences" in {
      val connector = preferenceConnector

      1 to circuitBreakerNumberOfCallsToTriggerStateChange foreach { _ =>
        mockHttpGET(Future failed new InternalServerException("some exception"))

        intercept[InternalServerException] {
          await(connector.getPreferences())
        }
      }

      intercept[UnhealthyServiceException] {
        await(connector.getPreferences())
      }
    }
  }

  "paperlessSettings()" should {
    val email                     = EmailAddress("me@mine.com")
    val paperlessSettingsAccepted = Paperless(TermsAccepted(Some(true)), email, Some("en"))
    val paperlessSettingsRejected = Paperless(TermsAccepted(Some(false)), email, Some("en"))

    def mockHttpPOST(
      paperlessSettings: Paperless,
      response:          Future[HttpResponse]
    ) =
      (http
        .POST(_: String, _: Paperless, _: Seq[(String, String)])(_: Writes[Paperless],
                                                                 _: HttpReads[HttpResponse],
                                                                 _: HeaderCarrier,
                                                                 _: ExecutionContext))
        .expects(termsAndCondtionssPostUrl, paperlessSettings, *, *, *, *, *)
        .returning(response)

    "update record to opted in when terms are accepted" in {
      mockHttpPOST(paperlessSettingsAccepted, Future successful HttpResponse(200, None))

      await(preferenceConnector.paperlessSettings(paperlessSettingsAccepted)) shouldBe PreferencesExists
    }

    "update record to opted out when terms are rejected" in {
      mockHttpPOST(paperlessSettingsRejected, Future successful HttpResponse(200, None))

      await(preferenceConnector.paperlessSettings(paperlessSettingsRejected)) shouldBe PreferencesExists
    }

    "create opt in record when terms are accepted" in {
      mockHttpPOST(paperlessSettingsAccepted, Future successful HttpResponse(201, None))

      await(preferenceConnector.paperlessSettings(paperlessSettingsAccepted)) shouldBe PreferencesCreated
    }

    "create opt out record when terms are rejected" in {
      mockHttpPOST(paperlessSettingsRejected, Future successful HttpResponse(201, None))

      await(preferenceConnector.paperlessSettings(paperlessSettingsRejected)) shouldBe PreferencesCreated
    }

    "report failure for unexpected response code when terms are accepted" in {
      mockHttpPOST(paperlessSettingsAccepted, Future successful HttpResponse(204, None))

      await(preferenceConnector.paperlessSettings(paperlessSettingsAccepted)) shouldBe PreferencesFailure
    }

    "report failure for unexpected response code when terms are rejected" in {
      mockHttpPOST(paperlessSettingsRejected, Future successful HttpResponse(204, None))

      await(preferenceConnector.paperlessSettings(paperlessSettingsRejected)) shouldBe PreferencesFailure
    }

    "throw an exception if the call fails" in {
      mockHttpPOST(paperlessSettingsRejected, Future failed Upstream5xxResponse("error", 500, 500))

      intercept[Upstream5xxResponse] {
        await(preferenceConnector.paperlessSettings(paperlessSettingsRejected))
      }
    }
  }

  "paperlessOptOut()" should {
    def mockHttpPOST(response: Future[HttpResponse]) =
      (http
        .POST(_: String, _: PaperlessOptOut, _: Seq[(String, String)])(_: Writes[PaperlessOptOut],
                                                                       _: HttpReads[HttpResponse],
                                                                       _: HeaderCarrier,
                                                                       _: ExecutionContext))
        .expects(termsAndCondtionssPostUrl, PaperlessOptOut(TermsAccepted(Some(false)), Some("en")), *, *, *, *, *)
        .returning(response)

    "update record to opted out" in {
      mockHttpPOST(Future successful HttpResponse(200, None))

      await(preferenceConnector.paperlessOptOut(PaperlessOptOut(TermsAccepted(Some(false)), Some("en")))) shouldBe PreferencesExists
    }

    "create opt out record" in {
      mockHttpPOST(Future successful HttpResponse(201, None))

      await(preferenceConnector.paperlessOptOut(PaperlessOptOut(TermsAccepted(Some(false)), Some("en")))) shouldBe PreferencesCreated
    }

    "report failure for unexpected response code" in {
      mockHttpPOST(Future successful HttpResponse(204, None))

      await(preferenceConnector.paperlessOptOut(PaperlessOptOut(TermsAccepted(Some(false)), Some("en")))) shouldBe PreferencesFailure
    }

    "report PreferencesDoesNotExist when not found" in {
      mockHttpPOST(Future successful HttpResponse(404, None))

      await(preferenceConnector.paperlessOptOut(PaperlessOptOut(TermsAccepted(Some(false)), Some("en")))) shouldBe PreferencesDoesNotExist
    }

    "throw an exception if the call fails" in {
      mockHttpPOST(Future failed Upstream5xxResponse("error", 500, 500))

      intercept[Upstream5xxResponse] {
        await(preferenceConnector.paperlessOptOut(PaperlessOptOut(TermsAccepted(Some(false)), Some("en"))))
      }
    }
  }

}
