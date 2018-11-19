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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.Verified
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

class SandboxCustomerProfileISpec extends BaseISpec {
  private val acceptJsonHeader = "Accept" -> "application/vnd.hmrc.1.0+json"
  private val nino = Nino("CS700100A")
  private val journeyId = "journeyId"
  private val invalidJsonBody: JsValue = Json.parse("{}")

  def request(url: String, sandboxControl: Option[String] = None, journeyId: Option[String] = None): WSRequest = {
    val urlMaybeId = journeyId.fold(url) { id => s"$url?journeyId=$id" }
    wsUrl(urlMaybeId).withHeaders(acceptJsonHeader, "SANDBOX-CONTROL" -> s"${sandboxControl.getOrElse("")}", "X-MOBILE-USER-ID" -> "208606423740")
  }

  def requestWithoutAcceptHeader(url: String): WSRequest = {
    wsUrl(url).withHeaders("X-MOBILE-USER-ID" -> "208606423740")
  }

  "GET /sandbox/profile/accounts  " should {
    val url = "/profile/accounts"

    "return the default account details by default without journey id" in {
      val response = await(request(url).get())
      response.status shouldBe 200
      response.json shouldBe toJson(Accounts(Some(nino), None, routeToIV = false, routeToTwoFactor = false, ""))
    }

    "return the default account details by default with journey id" in {
      val response = await(request(url, None, Some(journeyId)).get())
      response.status shouldBe 200
      response.json shouldBe toJson(Accounts(Some(nino), None, routeToIV = false, routeToTwoFactor = false, journeyId))
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401")).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403")).get())
      response.status shouldBe 403
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500")).get())
      response.status shouldBe 500
    }
  }

  "GET /sandbox/profile/personal-details/:nino" should {
    val url = s"/profile/personal-details/${nino.value}"

    val expectedDetails =
      PersonDetails(
        "etag",
        Person(Some("Jennifer"), None, Some("Thorsteinson"), None, Some("Ms"), None, Some("Female"), Some(parse("1999-01-31")), Some(nino)),
        Some(Address(Some("999 Big Street"), Some("Worthing"), Some("West Sussex"), None, None, Some("BN99 8IG"), None, None, None)))

    "return the default personal details without journey id" in {
      val response = await(request(url).get())
      response.status shouldBe 200
      response.json shouldBe toJson(expectedDetails)
    }

    "return the default personal details with journey id" in {
      val response = await(request(url, None, Some(journeyId)).get())
      response.status shouldBe 200
      response.json shouldBe toJson(expectedDetails)
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401")).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403")).get())
      response.status shouldBe 403
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500")).get())
      response.status shouldBe 500
    }
  }

  "GET /sandbox/profile/preferences" should {
    val url = "/profile/preferences"
    val expectedPreference = Preference(digital = true, Some(EmailPreference(EmailAddress("name@email.co.uk"), Verified)))

    "return the default personal details without a journeyId" in {
      val response = await(request(url).get)
      response.status shouldBe 200
      response.json shouldBe toJson(expectedPreference)
    }

    "return the default personal details with a journeyId" in {
      val response = await(request(url, None, Some(journeyId)).get)
      response.status shouldBe 200
      response.json shouldBe toJson(expectedPreference)
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401")).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403")).get())
      response.status shouldBe 403
    }

    "return 403 for ERROR-404" in {
      val response = await(request(url, Some("ERROR-404")).get())
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500")).get())
      response.status shouldBe 500
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-in" should {
    val url = "/profile/preferences/paperless-settings/opt-in"
    val paperlessSettings = toJson(Paperless(generic = TermsAccepted(true), email = EmailAddress("new-email@new-email.new.email")))

    "return a 200 response without a journeyId by default" in {
      val response = await(request(url).post(paperlessSettings))
      response.status shouldBe 200
    }

    "return a 200 response with a journeyId by default" in {
      val response = await(request(url, Some(journeyId)).post(paperlessSettings))
      response.status shouldBe 200
    }

    "return a 201 response for PREFERENCE-CREATED" in {
      val response = await(request(url, Some("PREFERENCE-CREATED")).post(paperlessSettings))
      response.status shouldBe 201
    }

    "return 400 for invalid form" in {
      val response = await(request(url).post(invalidJsonBody))
      response.status shouldBe 400
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401")).post(paperlessSettings))
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403")).post(paperlessSettings))
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response = await(request(url, Some("ERROR-404")).post(paperlessSettings))
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url).post(paperlessSettings))
      response.status shouldBe 406
    }

    "return a 409 response for ERROR-409" in {
      val response = await(request(url, Some("ERROR-409")).post(paperlessSettings))
      response.status shouldBe 409
    }

    "return a 500 response for ERROR-500" in {
      val response = await(request(url, Some("ERROR-500")).post(paperlessSettings))
      response.status shouldBe 500
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-out" should {
    val url = "/profile/preferences/paperless-settings/opt-out"
    val emptyBody = ""

    "return a 200 response by default without journeyId" in {
      val response = await(request(url).post(emptyBody))
      response.status shouldBe 200
    }

    "return a 200 response by default with journeyId" in {
      val response = await(request(url, None, Some(journeyId)).post(emptyBody))
      response.status shouldBe 200
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401")).post(emptyBody))
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403")).post(emptyBody))
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response = await(request(url, Some("ERROR-404")).post(emptyBody))
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url).post(emptyBody))
      response.status shouldBe 406
    }

    "return a 500 response for ERROR-500" in {
      val response = await(request(url, Some("ERROR-500")).post(emptyBody))
      response.status shouldBe 500
    }
  }

}
