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

import java.time.LocalDate

import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.customerprofile.domain.StatusName.{Bounced, ReOptIn, Verified, Pending}
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

class SandboxCustomerProfileISpec extends BaseISpec {
  private val acceptJsonHeader = "Accept" -> "application/vnd.hmrc.1.0+json"
  private val nino             = Nino("CS700100A")
  private val journeyId        = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  private val invalidJsonBody: JsValue = Json.parse("{}")

  def request(
    url:            String,
    sandboxControl: Option[String] = None,
    journeyId:      String
  ): WSRequest =
    wsUrl(s"$url?journeyId=$journeyId")
      .addHttpHeaders(
        acceptJsonHeader,
        "SANDBOX-CONTROL"  -> s"${sandboxControl.getOrElse("")}",
        "X-MOBILE-USER-ID" -> "208606423740"
      )

  def requestWithoutAcceptHeader(
    url:       String,
    journeyId: String
  ): WSRequest =
    wsUrl(s"$url?journeyId=$journeyId")
      .addHttpHeaders("X-MOBILE-USER-ID" -> "208606423740")

  def requestWithoutJourneyId(
    url:            String,
    sandboxControl: Option[String] = None
  ): WSRequest =
    wsUrl(s"$url").addHttpHeaders(
      acceptJsonHeader,
      "SANDBOX-CONTROL"  -> s"${sandboxControl.getOrElse("")}",
      "X-MOBILE-USER-ID" -> "208606423740"
    )

  "GET /sandbox/profile/accounts  " should {
    val url = "/profile/accounts"

    "return the default account details by default with journey id" in {
      val response = await(request(url, None, journeyId).get())
      response.status shouldBe 200
      response.json shouldBe toJson(
        Accounts(
          Some(nino),
          None,
          routeToIV        = false,
          routeToTwoFactor = false,
          journeyId
        )
      )
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401"), journeyId).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403"), journeyId).get())
      response.status shouldBe 403
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, journeyId).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500"), journeyId).get())
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(requestWithoutJourneyId("/profile/accounts", None).get())
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          "/profile/accounts?journeyId=ThisIsAnInvalidJourneyId",
          None
        ).get()
      )
      response.status shouldBe 400
    }
  }

  "GET /sandbox/profile/personal-details/:nino" should {
    val url = s"/profile/personal-details/${nino.value}"

    val expectedDetails =
      PersonDetails(
        Person(
          Some("Jennifer"),
          None,
          Some("Thorsteinson"),
          None,
          Some("Ms"),
          None,
          Some("Female"),
          Option(LocalDate.parse("1999-01-31")),
          Some(nino)
        ),
        Some(
          Address(
            Some("999 Big Street"),
            Some("Worthing"),
            Some("West Sussex"),
            None,
            None,
            Some("BN99 8IG"),
            None,
            None,
            None
          )
        )
      )

    "return the default personal details with journey id" in {
      val response = await(request(url, None, journeyId).get())
      response.status shouldBe 200
      response.json   shouldBe toJson(expectedDetails)
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401"), journeyId).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403"), journeyId).get())
      response.status shouldBe 403
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, journeyId).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500"), journeyId).get())
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          s"/profile/personal-details/${nino.value}",
          None
        ).get()
      )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          s"/profile/personal-details/${nino.value}?journeyId=ThisIsAnInvalidJourneyId",
          None
        ).get()
      )
      response.status shouldBe 400
    }
  }

  "GET /sandbox/profile/preferences" should {
    val url = "/profile/preferences"
    val expectedPreference =
      Preference(
        digital = false,
        None
      )

    def preferencesSandbox(
      status:   StatusName,
      linkSent: Option[org.joda.time.LocalDate] = None
    ) =
      Preference(
        digital      = true,
        emailAddress = Some("jt@test.com"),
        email        = Some(EmailPreference(email = EmailAddress("jt@test.com"), status = status, linkSent = linkSent)),
        status       = Some(PaperlessStatus(status, Category.ActionRequired)),
        linkSent     = linkSent
      )

    "return the default preferences with a journeyId" in {
      val response = await(request(url, None, journeyId).get)
      response.status shouldBe 200
      response.json   shouldBe toJson(expectedPreference)
    }

    "return the verified preferences for VERIFIED" in {
      val response = await(request(url, Some("VERIFIED"), journeyId).get)
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(Verified))
    }

    "return the unverified preferences for UNVERIFIED" in {
      val response = await(request(url, Some("UNVERIFIED"), journeyId).get)
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(Pending, Some(org.joda.time.LocalDate.now())))
    }

    "return the bounced preferences for BOUNCED" in {
      val response = await(request(url, Some("BOUNCED"), journeyId).get)
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(Bounced))
    }

    "return the reOptIn preferences for REOPTIN" in {
      val response = await(request(url, Some("REOPTIN"), journeyId).get)
      response.status shouldBe 200
      response.json   shouldBe toJson(preferencesSandbox(ReOptIn))
    }

    "return 401 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-401"), journeyId).get())
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(request(url, Some("ERROR-403"), journeyId).get())
      response.status shouldBe 403
    }

    "return 403 for ERROR-404" in {
      val response = await(request(url, Some("ERROR-404"), journeyId).get())
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, journeyId).get())
      response.status shouldBe 406
    }

    "return 500 for ERROR-401" in {
      val response = await(request(url, Some("ERROR-500"), journeyId).get())
      response.status shouldBe 500
    }

    "return 400 if invalid journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences?journeyId=ThisIsAnInvalidJourneyId",
            None
          ).get()
        )
      response.status shouldBe 400
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-in" should {
    val url = "/profile/preferences/paperless-settings/opt-in"
    val paperlessSettings =
      toJson(
        Paperless(
          generic = TermsAccepted(Some(true)),
          email   = EmailAddress("new-email@new-email.new.email"),
          Some(English)
        )
      )

    "return a 204 response with a journeyId by default" in {
      val response =
        await(request(url, None, journeyId).post(paperlessSettings))
      response.status shouldBe 204
    }

    "return a 201 response for PREFERENCE-CREATED" in {
      val response = await(
        request(url, Some("PREFERENCE-CREATED"), journeyId)
          .post(paperlessSettings)
      )
      response.status shouldBe 201
    }

    "return 400 for invalid form" in {
      val response = await(request(url, None, journeyId).post(invalidJsonBody))
      response.status shouldBe 400
    }

    "return 401 for ERROR-401" in {
      val response = await(
        request(url, Some("ERROR-401"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response = await(
        request(url, Some("ERROR-403"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response = await(
        request(url, Some("ERROR-404"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response = await(
        requestWithoutAcceptHeader(url, journeyId).post(paperlessSettings)
      )
      response.status shouldBe 406
    }

    "return a 409 response for ERROR-409" in {
      val response = await(
        request(url, Some("ERROR-409"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 409
    }

    "return a 500 response for ERROR-500" in {
      val response = await(
        request(url, Some("ERROR-500"), journeyId).post(paperlessSettings)
      )
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-in",
            None
          ).post(paperlessSettings)
        )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response = await(
        requestWithoutJourneyId(
          "/profile/preferences/paperless-settings/opt-in?journeyId=ThisIsAnInvalidJourneyId",
          None
        ).post(paperlessSettings)
      )
      response.status shouldBe 400
    }
  }

  "POST /sandbox/preferences/profile/paperless-settings/opt-out" should {
    val url       = "/profile/preferences/paperless-settings/opt-out"
    val emptyBody = ""

    val paperlessSettings =
      toJson(
        PaperlessOptOut(
          generic = Some(TermsAccepted(Some(false))),
          Some(English)
        )
      )

    "return a 204 response by default with journeyId" in {
      val response = await(request(url, None, journeyId).post(paperlessSettings))
      response.status shouldBe 204
    }

    "return 401 for ERROR-401" in {
      val response =
        await(request(url, Some("ERROR-401"), journeyId).post(paperlessSettings))
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response =
        await(request(url, Some("ERROR-403"), journeyId).post(paperlessSettings))
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response =
        await(request(url, Some("ERROR-404"), journeyId).post(paperlessSettings))
      response.status shouldBe 404
    }

    "return 406 without Accept header" in {
      val response =
        await(requestWithoutAcceptHeader(url, journeyId).post(paperlessSettings))
      response.status shouldBe 406
    }

    "return a 500 response for ERROR-500" in {
      val response =
        await(request(url, Some("ERROR-500"), journeyId).post(paperlessSettings))
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-out",
            None
          ).post(paperlessSettings)
        )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-out?journeyId=ThisIsAnInvalidJourneyId",
            None
          ).post(paperlessSettings)
        )
      response.status shouldBe 400
    }
  }

  "POST /sandbox/profile/preferences/pending-email" should {
    val url = "/profile/preferences/pending-email"
    val changeEmail =
      toJson(ChangeEmail(email = EmailAddress("new-email@new-email.new.email")))

    "return a 204 response with a journeyId by default" in {
      val response = await(request(url, None, journeyId).post(changeEmail))
      response.status shouldBe 204
    }

    "return 400 for invalid form" in {
      val response = await(request(url, None, journeyId).post(invalidJsonBody))
      response.status shouldBe 400
    }

    "return 401 for ERROR-401" in {
      val response =
        await(request(url, Some("ERROR-401"), journeyId).post(changeEmail))
      response.status shouldBe 401
    }

    "return 403 for ERROR-403" in {
      val response =
        await(request(url, Some("ERROR-403"), journeyId).post(changeEmail))
      response.status shouldBe 403
    }

    "return a 404 response for ERROR-404" in {
      val response =
        await(request(url, Some("ERROR-404"), journeyId).post(changeEmail))
      response.status shouldBe 404
    }

    "return a 409 response for ERROR-409" in {
      val response =
        await(request(url, Some("ERROR-409"), journeyId).post(changeEmail))
      response.status shouldBe 409
    }

    "return a 500 response for ERROR-500" in {
      val response =
        await(request(url, Some("ERROR-500"), journeyId).post(changeEmail))
      response.status shouldBe 500
    }

    "return 400 if no journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-in",
            None
          ).post(changeEmail)
        )
      response.status shouldBe 400
    }

    "return 400 if invalid journeyId is supplied" in {
      val response =
        await(
          requestWithoutJourneyId(
            "/profile/preferences/paperless-settings/opt-in?journeyId=ThisIsAnInvalidJourneyId",
            None
          ).post(changeEmail)
        )
      response.status shouldBe 400
    }
  }

}
