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

package uk.gov.hmrc.customerprofile.controllers.action

import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L200, L50}
import uk.gov.hmrc.customerprofile.domain.Accounts
import uk.gov.hmrc.customerprofile.domain.CredentialStrength.{Strong, Weak}
import uk.gov.hmrc.customerprofile.stubs.AuthStub._
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class AccountAccessControlISpec extends BaseISpec with Eventually  {

  implicit val hc = HeaderCarrier()

  val saUtr = SaUtr("1872796160")
  val nino = Nino("CS100700A")

  def authConnector(response : HttpResponse, cl: ConfidenceLevel = L200) = new AccountAccessControl {
  }

  "Returning the accounts" should {
    "be found and routeToIV and routeToTwoFactor should be true" in {
      accountsFound(nino, L50, Weak, saUtr)

      val accounts: Accounts =  await(AccountAccessControl.accounts(hc))
      accounts.nino.get shouldBe nino
      accounts.saUtr.get shouldBe saUtr
      accounts.routeToIV shouldBe true
      accounts.routeToTwoFactor shouldBe true
    }

    "be found and routeToIV is false and routeToTwoFactor is true" in {
      accountsFound(nino, L50, Strong, saUtr)

      val accounts: Accounts =  await(AccountAccessControl.accounts(hc))
      accounts.nino.get shouldBe nino
      accounts.saUtr.get shouldBe saUtr
      accounts.routeToIV shouldBe true
      accounts.routeToTwoFactor shouldBe false
    }

    "be found and routeToIV and routeToTwoFactor should be false" in {
      accountsFound(nino, L200, Strong, saUtr)

      val accounts: Accounts =  await(AccountAccessControl.accounts(hc))
      accounts.nino.get shouldBe nino
      accounts.saUtr.get shouldBe saUtr
      accounts.routeToIV shouldBe false
      accounts.routeToTwoFactor shouldBe false
    }

    "be found when the users account does not have a NINO" in {
      accountsFoundWithoutNino(L200, Strong, saUtr)

      val accounts: Accounts =  await(AccountAccessControl.accounts(hc))
      accounts.nino shouldBe None
      accounts.saUtr.get shouldBe saUtr
      accounts.routeToIV shouldBe false
      accounts.routeToTwoFactor shouldBe false
    }
  }

  "grantAccess" should {
    "error with unauthorised when account has low CL" in {
      authRecordExists(nino, L50)

      intercept[AccountWithLowCL] {
        await(AccountAccessControl.grantAccess(Some(nino)))
      }
    }

    "find NINO only account when cCL is correct" in {
      authRecordExists(nino, L200)
      await(AccountAccessControl.grantAccess(Some(nino)))
    }

    "fail to return authority when no NINO exists" in {
      authRecordExistsWithoutNino

      intercept[NinoNotFoundOnAccount] {
        await(AccountAccessControl.grantAccess(Some(nino)))
      }
    }

    "fail to return authority when auth NINO does not match request NINO" in {
      authRecordExists(nino, L200)

      intercept[FailToMatchTaxIdOnAuth] {
        await(AccountAccessControl.grantAccess(Some(Nino("CS333700A"))))
      }
    }
  }
}
