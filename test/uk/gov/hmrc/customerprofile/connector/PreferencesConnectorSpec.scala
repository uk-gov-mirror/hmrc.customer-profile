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
import play.api.http.Status._
import play.api.libs.json.Writes
import uk.gov.hmrc.circuitbreaker.CircuitBreakerConfig
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PreferencesConnectorSpec extends UnitSpec {

  implicit val hc = HeaderCarrier()
  class TestPreferencesConnector extends PreferencesConnector with ServicesConfig with ServicesCircuitBreaker {
    val serviceUrl = "http://preferences.service/"
    override protected val externalServiceName: String = "preferences"
    override protected def circuitBreakerConfig = CircuitBreakerConfig(externalServiceName, 5, 2000, 2000)
    def http : HttpPut = ???
  }

  def preferenceConnector(response: HttpResponse) = new TestPreferencesConnector {
    self: ServicesCircuitBreaker ⇒
    override def http: HttpPut = new HttpPut {
      override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        Future.successful(response)
      }
      override def configuration: Option[Config] = None
      override val hooks: Seq[HttpHook] = Seq.empty
    }
  }

  "preferences connector" should {
    "/preferences/:entityId/pending-email should return status EmailUpdateOk when the service returns an OK status" in {
      val changeEmailRequest = ChangeEmail(email = "some-new-email@newEmail.new.email")
      val preferencesResponse = HttpResponse(responseStatus = OK)
      val response = await(preferenceConnector(preferencesResponse).updatePendingEmail(changeEmailRequest, "some-entity-id"))
      response shouldBe EmailUpdateOk
    }

    "/preferences/:entityId/pending-email should return status EmailNotExist when the service returns an CONFLICT status" in {
      val changeEmailRequest = ChangeEmail(email = "some-new-email@newEmail.new.email")
      val preferencesResponse = HttpResponse(responseStatus = CONFLICT)
      val response = await(preferenceConnector(preferencesResponse).updatePendingEmail(changeEmailRequest, "some-entity-id"))
      response shouldBe EmailNotExist
    }

    "/preferences/:entityId/pending-email should return status NoPreferenceExists when the service returns an NOT_FOUND status" in {
      val changeEmailRequest = ChangeEmail(email = "some-new-email@newEmail.new.email")
      val preferencesResponse = HttpResponse(responseStatus = NOT_FOUND)
      val response = await(preferenceConnector(preferencesResponse).updatePendingEmail(changeEmailRequest, "some-entity-id"))
      response shouldBe NoPreferenceExists
    }

    "/preferences/:entityId/pending-email should return status EmailUpdateFailed when the service returns any other 4xxResponse" in {
      val changeEmailRequest = ChangeEmail(email = "some-new-email@newEmail.new.email")
      val preferencesResponse = HttpResponse(responseStatus = TOO_MANY_REQUESTS)
      val response = await(preferenceConnector(preferencesResponse).updatePendingEmail(changeEmailRequest, "some-entity-id"))
      response shouldBe EmailUpdateFailed
    }

    "/preferences/:entityId/pending-email should return status EmailUpdateFailed when the service returns any status NOT 200" in {
      val changeEmailRequest = ChangeEmail(email = "some-new-email@newEmail.new.email")
      val preferencesResponse = HttpResponse(responseStatus = SERVICE_UNAVAILABLE)
      val response = await(preferenceConnector(preferencesResponse).updatePendingEmail(changeEmailRequest, "some-entity-id"))
      response shouldBe EmailUpdateFailed
    }
  }

}
