/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.domain

import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.http.UnauthorizedException
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

class CustomerProfileSpec extends UnitSpec with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  "CustomerProfile" should {
    "be created" in {
      val nino = Some(Nino("CS100700A"))
      val saUtr = Some(SaUtr("1872796160"))
      def accounts: () => Future[Accounts] = () => Future.successful(Accounts(nino, saUtr))

      def pd(nino: Option[Nino]): Future[PersonDetails] = {
        val person = Person(Some("Mike"), None, Some("Potter"), None, Some("Mr"), None, Some("M"), Some(DateTimeUtils.now), nino)
        Future.successful(PersonDetails("etag1234", person, None, None))
      }

      val customerProfile = await(CustomerProfile.create(accounts, pd))

      customerProfile.accounts.nino.get shouldBe nino.get
      customerProfile.accounts.saUtr.get shouldBe saUtr.get

      customerProfile.personalDetails.etag shouldBe "etag1234"
      customerProfile.personalDetails.person.firstName.get shouldBe "Mike"
    }

    "fail to create" in {

      def accounts: () => Future[Accounts] = () => Future.successful( throw new UnauthorizedException("User does not have sufficient permissions"))

      def pd(nino: Option[Nino]): Future[PersonDetails] = {
        val person = Person(Some("Mike"), None, Some("Potter"), None, Some("Mr"), None, Some("M"), Some(DateTimeUtils.now), nino)
        Future.successful(PersonDetails("etag1234", person, None, None))
      }

      a [UnauthorizedException] shouldBe thrownBy {
        CustomerProfile.create(accounts, pd)
      }
    }
  }
}
