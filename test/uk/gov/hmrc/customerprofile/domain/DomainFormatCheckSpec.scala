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

package uk.gov.hmrc.customerprofile.domain

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpecLike}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{prettyPrint, toJson}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.util.Random

class DomainFormatCheckSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout {

  import DomainGenerator._

  "Accounts" in {
    Logger.debug(
      "Accounts response : " + prettyPrint(accountsWithNinoAndSaUtrAsJson)
    )
  }

  "Personal details" in {
    Logger.debug(
      "Personal details response : " + prettyPrint(personalDetailsAsJson)
    )
  }

  "Paperless" in {
    Logger.debug("Paperless request : " + prettyPrint(paperlessAsJson))
  }

  "Paperless opt out" in {
    Logger.debug(
      "Paperless opt out response : " + prettyPrint(paperlessOptOutAsJson)
    )
  }

  "Verified email Preference" in {
    Logger.debug(
      "Preference response : " + prettyPrint(verifiedEmailPreferenceAsJson)
    )
  }
}

object DomainGenerator {

  import uk.gov.hmrc.domain.Generator

  val nino: Nino = new Generator().nextNino
  val saUtr: SaUtr = new SaUtrGenerator().nextSaUtr

  val accountsWithNinoAndSaUtr = Accounts(
    Some(nino),
    Some(saUtr),
    routeToIV = false,
    routeToTwoFactor = false,
    "102030394AAA"
  )

  lazy val accountsWithNinoAndSaUtrAsJson: JsValue = toJson(
    accountsWithNinoAndSaUtr
  )

  val email = EmailAddress("name@email.co.uk")

  val paperless = Paperless(TermsAccepted(Some(true)), email, Some("en"))
  lazy val paperlessAsJson: JsValue = toJson(paperless)

  val paperlessOptOut = PaperlessOptOut(TermsAccepted(Some(false)), Some("en"))
  lazy val paperlessOptOutAsJson: JsValue = toJson(paperlessOptOut)

  val verifiedEmailPreference =
    Preference(digital = true, Some(EmailPreference(email, Status.Verified)))

  lazy val verifiedEmailPreferenceAsJson: JsValue = toJson(
    verifiedEmailPreference
  )

  val person =
    Person(
      Some("John"),
      Some("Albert"),
      Some("Smith"),
      None,
      Some("Mr"),
      None,
      Some("M"),
      Some(LocalDate.now.minusYears(30)),
      Some(nino)
    )
  val address: Option[Address] = None
  val personalDetails = PersonDetails(person, address)
  lazy val personalDetailsAsJson: JsValue = toJson(personalDetails)

}

//TODO add this to domain
sealed class SaUtrGenerator(random: Random = new Random) {
  def this(seed: Int) = this(new Random(seed))

  def randomNext: Int = random.nextInt(1000000)

  def nextSaUtr: SaUtr = SaUtr(randomNext.toString)
}
