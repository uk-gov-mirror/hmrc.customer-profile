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
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnectorSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with MockFactory {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val nino:      Nino                    = Nino("CS700100A")
  val http:      CoreGet                 = mock[CoreGet]
  val connector: CitizenDetailsConnector = new CitizenDetailsConnector("someUrl", http)

  def mockHttpGet(exception: Exception) =
    (http
      .GET(_: String)(_: HttpReads[HttpResponse], _: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(Future failed exception)

  "citizenDetailsConnector" should {
    "throw BadRequestException when a 400 response is returned" in {
      mockHttpGet(new BadRequestException("bad request"))

      intercept[BadRequestException] {
        await(connector.personDetails(nino))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      mockHttpGet(Upstream5xxResponse("Error", 500, 500))

      intercept[Upstream5xxResponse] {
        await(connector.personDetails(nino))
      }
    }
  }
}
