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

import org.joda.time.DateTime.parse
import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.customerprofile.controllers.UpgradeRequired
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.Verified
import uk.gov.hmrc.customerprofile.domain.NativeOS.iOS
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

class SandboxCustomerProfileISpec extends BaseISpec {
  private val headerToRedirectToSandbox = "X-MOBILE-USER-ID" -> "208606423740"
  private val acceptJsonHeader = "Accept" -> "application/vnd.hmrc.1.0+json"
  private val nino = Nino("CS700100A")

  "GET /sandbox//profile/accounts  " should {
    def request: WSRequest = wsUrl("/profile/accounts").withHeaders(headerToRedirectToSandbox, acceptJsonHeader)

    "return the default account details" in {
      val response = await(request.get())
      response.status shouldBe 200
      (response.json \ "nino").as[String] shouldBe nino.nino // journey id is random so can't test
    }
  }

  "GET /sandbox/profile/personal-details/:nino" should {
    def request(nino: Nino): WSRequest = wsUrl(s"/profile/personal-details/${nino.value}").withHeaders(headerToRedirectToSandbox, acceptJsonHeader)

    "return the default personal details" in {
      val expectedDetails =
        PersonDetails(
          "etag",
          Person(Some("Jennifer"), None, Some("Thorsteinson"), None, Some("Ms"), None, Some("Female"), Some(parse("1999-01-31")), Some(nino)),
          Some(Address(Some("999 Big Street"), Some("Worthing"), Some("West Sussex"), None, None, Some("BN99 8IG"), None, None, None)))

      val response = await(request(nino).get())
      response.status shouldBe 200
      response.json shouldBe toJson(expectedDetails)
    }
  }

  "GET /sandbox/profile/preferences" should {
    def request: WSRequest = wsUrl("/profile/preferences").withHeaders(headerToRedirectToSandbox, acceptJsonHeader)

    "return the default personal details" in {
      val expectedPreference = Preference(digital = true, Some(EmailPreference(EmailAddress("name@email.co.uk"), Verified)))

      val response = await(request.get())
      response.status shouldBe 200
      response.json shouldBe toJson(expectedPreference)
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-in" should {
    def request: WSRequest = wsUrl("/profile/preferences/paperless-settings/opt-in").withHeaders(headerToRedirectToSandbox, acceptJsonHeader)

    val paperlessSettings = toJson(Paperless(generic = TermsAccepted(true), email = EmailAddress("new-email@new-email.new.email")))

    "return a 200 response" in {
      val response = await(request.post(paperlessSettings))
      response.status shouldBe 200
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-out" should {
    def request: WSRequest = wsUrl("/profile/preferences/paperless-settings/opt-out").withHeaders(headerToRedirectToSandbox, acceptJsonHeader)

    "return a 200 response" in {
      val response = await(request.post(""))
      response.status shouldBe 200
    }
  }

  "POST /sandbox/profile/native-app/version-check" should {
    def request: WSRequest = wsUrl("/profile/native-app/version-check").withHeaders(headerToRedirectToSandbox, acceptJsonHeader)

    "return a 200 response with upfgrade required false" in {
      val deviceVersion = toJson(DeviceVersion(iOS, "1.0"))

      val response = await(request.post(deviceVersion))
      response.status shouldBe 200
      response.json shouldBe toJson(UpgradeRequired(false))
    }
  }
}
