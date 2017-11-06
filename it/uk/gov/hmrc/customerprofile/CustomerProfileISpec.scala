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
import play.api.libs.json.Json
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customerprofile.stubs.{AuthStub, CitizenDetailsStub}
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino
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

    "return 500 response status code when citizen-details returns 423 response status code." in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.citizenDetailsErrorResponse(nino, 423)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 500
      }
      response.json shouldBe Json.parse("""{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}""".stripMargin)
    }

    "return 500 response status code when citizen-details returns 500 response status code." in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.citizenDetailsErrorResponse(nino, 500)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 500
      }
      response.json shouldBe Json.parse("""{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}""".stripMargin)
    }

    "return 404 response status code when citizen-details returns 404 response status code." in {
      val nino = Nino("AA000006C")
      CitizenDetailsStub.citizenDetailsErrorResponse(nino, 404)
      AuthStub.authRecordExists(nino)

      val response = await(wsUrl(s"/profile/personal-details/${nino.value}")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        .get())

      withClue(response.body) {
        response.status shouldBe 404
      }
      response.json shouldBe None
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
