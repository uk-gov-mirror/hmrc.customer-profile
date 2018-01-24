/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile

import java.io.InputStream

import org.scalatest.concurrent.Eventually
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.customerprofile.stubs.{AuthStub, CitizenDetailsStub, EntityResolverStub, PreferencesStub}
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino

import scala.io.Source

class CustomerProfileISpec extends BaseISpec with Eventually {
  "GET /profile/personal-details/:nino" should {
    "return personal details for the given NINO from citizen-details" in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.designatoryDetailsForNinoAre(nino, resourceAsString("AA000006C-citizen-details.json").get)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 200
      }
      response.json shouldBe getResourceAsJsValue("expected-AA000006C-personal-details.json")
    }

    "return a 423 response status code when the NINO is locked due to Manual Correspondence Indicator flag being set in NPS" in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.npsDataIsLockedDueToMciFlag(nino)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 423
      }
      response.json shouldBe Json.parse("""{"code":"MANUAL_CORRESPONDENCE_IND","message":"Data cannot be disclosed to the user because MCI flag is set in NPS"}""")
    }

    "return 500 response status code when citizen-details returns 500 response status code." in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.designatoryDetailsWillReturnErrorResponse(nino, 500)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 500
      }
      response.json shouldBe Json.parse("""{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}""")
    }

    "return 404 response status code when citizen-details returns 404 response status code." in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.designatoryDetailsWillReturnErrorResponse(nino, 404)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 404
      }
      response.json shouldBe Json.parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")
    }
  }

  "PUT /profile/preferences/pending-email" should {
    "return a 200 response when a pending email preference is successfully added" in {
      val nino = Nino("AA000006C")
      val entityId = "1098561938451038465138465"
      val changeEmail = Json.toJson[ChangeEmail](ChangeEmail(email = "new-email@new-email.new.email"))
      EntityResolverStub.respondWithEntityDetailsByNino(nino.value, entityId)
      AuthStub.authRecordExists(nino)
      PreferencesStub.successfulPendingEmailUpdate(entityId)

      val response = await(wsUrl("/profile/preferences/pending-email")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .put(changeEmail))

      withClue(response.body) {
        response.status shouldBe 200
      }
    }

    "return a Conflict response when preferences has no existing verified or pending email" in {
      val nino = Nino("AA000006C")
      val entityId = "1098561938451038465138465"
      val changeEmail = Json.toJson[ChangeEmail](ChangeEmail(email = "new-email@new-email.new.email"))
      val expectedResponse = Json.parse("""{"code":"CONFLICT","message":"No existing verified or pending data"}""")
      EntityResolverStub.respondWithEntityDetailsByNino(nino.value, entityId)
      AuthStub.authRecordExists(nino)
      PreferencesStub.conflictPendingEmailUpdate(entityId)

      val response = await(wsUrl("/profile/preferences/pending-email")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .put(changeEmail))

      withClue(response.body) {
        response.status shouldBe 409
        response.json shouldBe expectedResponse
      }
    }

    "return a Not Found response when unable to find a preference to update for an entity" in {
      val nino = Nino("AA000006C")
      val entityId = "1098561938451038465138465"
      val changeEmail = Json.toJson[ChangeEmail](ChangeEmail(email = "new-email@new-email.new.email"))
      val expectedResponse = Json.parse("""{"code":"NOT_FOUND","message":"Resource was not found"}""")
      EntityResolverStub.respondWithEntityDetailsByNino(nino.value, entityId)
      AuthStub.authRecordExists(nino)
      PreferencesStub.notFoundPendingEmailUpdate(entityId)

      val response = await(wsUrl("/profile/preferences/pending-email")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .put(changeEmail))

      withClue(response.body) {
        response.status shouldBe 404
        response.json shouldBe expectedResponse
      }
    }

    "return a Internal Server Error response when unable update pending email preference for an entity" in {
      val nino = Nino("AA000006C")
      val entityId = "1098561938451038465138465"
      val changeEmail = Json.toJson[ChangeEmail](ChangeEmail(email = "new-email@new-email.new.email"))
      val expectedResponse = Json.parse("""{"code":"PREFERENCE_SETTINGS_ERROR","message":"Failed to set preferences"}""")
      EntityResolverStub.respondWithEntityDetailsByNino(nino.value, entityId)
      AuthStub.authRecordExists(nino)
      PreferencesStub.errorPendingEmailUpdate(entityId)

      val response = await(wsUrl("/profile/preferences/pending-email")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .put(changeEmail))

      withClue(response.body) {
        response.status shouldBe 500
        response.json shouldBe expectedResponse
      }
    }
  }

  private def resourceAsString(resourcePath: String): Option[String] =
    withResourceStream(resourcePath) { is =>
      Source.fromInputStream(is).mkString
    }

  private def getResourceAsString(resourcePath: String): String =
    resourceAsString(resourcePath).getOrElse(throw new RuntimeException(s"Could not find resource $resourcePath"))

  private def resourceAsJsValue(resourcePath: String): Option[JsValue] =
    withResourceStream(resourcePath) { is =>
      Json.parse(is)
    }

  private def getResourceAsJsValue(resourcePath: String): JsValue =
    resourceAsJsValue(resourcePath).getOrElse(throw new RuntimeException(s"Could not find resource $resourcePath"))

  private def withResourceStream[A](resourcePath: String)(f: (InputStream => A)): Option[A] =
    Option(getClass.getResourceAsStream(resourcePath)) map { is =>
      try {
        f(is)
      } finally {
        is.close()
      }
    }

}
