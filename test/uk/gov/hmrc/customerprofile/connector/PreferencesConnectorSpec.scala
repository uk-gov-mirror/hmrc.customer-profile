/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.Writes
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customerprofile.config.WSHttpImpl
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PreferencesConnectorSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with MockFactory {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val http:                WSHttpImpl    = mock[WSHttpImpl]
  val config:              Configuration = mock[Configuration]
  val environment:         Environment   = mock[Environment]
  val baseUrl:             String        = "baseUrl"
  val externalServiceName: String        = "externalServiceName"
  val entityId:            String        = "entityId"
  val changeEmailRequest:  ChangeEmail   = ChangeEmail(email = "some-new-email@newEmail.new.email")

  val connector: PreferencesConnector =
    new PreferencesConnector(http, baseUrl, externalServiceName, config, environment)

  "updatePendingEmail()" should {
    def mockPUT(response: Future[HttpResponse]) =
      (http
        .PUT(_: String, _: ChangeEmail, _: Seq[(String, String)])(_: Writes[ChangeEmail],
                                        _: HttpReads[HttpResponse],
                                        _: HeaderCarrier,
                                        _: ExecutionContext))
        .expects(s"$baseUrl/preferences/$entityId/pending-email", changeEmailRequest, *, *, *, *, *)
        .returns(response)

    "return status EmailUpdateOk when the service returns an OK status" in {
      mockPUT(Future successful HttpResponse(200))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe EmailUpdateOk
    }

    "handle 404 NOT_FOUND response" in {
      mockPUT(Future failed new NotFoundException("not found"))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe NoPreferenceExists
    }

    "handle 409 CONFLICT response" in {
      mockPUT(Future failed Upstream4xxResponse("not found", 409, 409))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe EmailNotExist
    }

    "handles exceptions" in {
      mockPUT(Future failed Upstream4xxResponse("I'm a teapot", 418, 418))

      val response = await(connector.updatePendingEmail(changeEmailRequest, entityId))
      response shouldBe EmailUpdateFailed
    }
  }
}
