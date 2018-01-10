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
import uk.gov.hmrc.customerprofile.domain.{Person, PersonDetails}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CitizenDetailsConnectorSpec
  extends UnitSpec
          with ScalaFutures {

  private trait Setup {

    implicit lazy val hc = HeaderCarrier()

    val person = PersonDetails("etag", Person(Some("Firstname"), Some("Lastname"),Some("Middle"),Some("Intial"),
      Some("Title"),Some("Honours"), Some("sex"),None, None), None)
    val nino = Nino("CS700100A")

    lazy val http500Response = Future.failed(Upstream5xxResponse("Error", 500, 500))
    lazy val http400Response = Future.failed(new BadRequestException("bad request"))
    lazy val http200Response = Future.successful(HttpResponse(200, None))//Some(Json.toJson(person))))

    lazy val response: Future[HttpResponse] = http400Response

    val connector = new CitizenDetailsConnector {
      override lazy val citizenDetailsConnectorUrl = "someUrl"
      override lazy val http: CoreGet = new CoreGet with HttpGet {
        override val hooks: Seq[HttpHook] = NoneRequired
        override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = response
        override def configuration: Option[Config] = None
      }
    }
  }

  "citizenDetailsConnector" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      override lazy val response = http400Response
        intercept[BadRequestException] {
          await(connector.personDetails(nino))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      override lazy val response = http500Response
      intercept[Upstream5xxResponse] {
        await(connector.personDetails(nino))
      }
    }

  }

}
