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

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Random

class DomainFormatCheckSpec extends UnitSpec {

  import DomainGenerator._

  "Accounts" in {
    Logger.debug("Accounts response : " + Json.prettyPrint(accountsAsJson))
  }

  "Paperless" in {
    Logger.debug("Paperless request : " + Json.prettyPrint(paperlessAsJson))
  }

  "verified email Preference" in {
    Logger.debug("Preference response : " + Json.prettyPrint(verifiedEmailPreferenceAsJson))
  }
}

object DomainGenerator {

  import uk.gov.hmrc.domain.Generator

  val nino = new Generator().nextNino
  val saUtr = new SaUtrGenerator().nextSaUtr

  val accounts = Accounts(Some(nino), Some(saUtr))
  lazy val accountsAsJson = Json.toJson(accounts)

  val email = EmailAddress("name@email.co.uk")

  val paperless = Paperless(TermsAccepted(true), email)
  lazy val paperlessAsJson = Json.toJson(paperless)

  val verifiedEmailPreference = Preference(true, Some(EmailPreference(email, Status.Verified)))
  lazy val verifiedEmailPreferenceAsJson = Json.toJson(verifiedEmailPreference)

}

//TODO add this to domain
sealed class SaUtrGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new scala.util.Random(seed))

  def randomNext = random.nextInt(1000000)

  def nextSaUtr: SaUtr = SaUtr(randomNext.toString)
}
