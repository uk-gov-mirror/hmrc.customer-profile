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

package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json.obj
import uk.gov.hmrc.auth.core.AuthenticateHeaderParser.{ENROLMENT, WWW_AUTHENTICATE}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.customerprofile.domain.CredentialStrength
import uk.gov.hmrc.customerprofile.domain.CredentialStrength.Strong
import uk.gov.hmrc.domain.{Nino, SaUtr}

object AuthStub {
  private val authUrl: String = "/auth/authorise"
  private val utr:     SaUtr  = SaUtr("1872796160")

  private val authorisationRequestJson: String =
    """{ "authorise": [], "retrieve": ["nino","confidenceLevel"] }""".stripMargin

  private val accountsRequestJson: String =
    """{ "authorise": [], "retrieve": ["nino","saUtr","credentialStrength","confidenceLevel"] }""".stripMargin

  def authRecordExists(
    nino:            Nino,
    confidenceLevel: ConfidenceLevel = L200
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(obj("confidenceLevel" -> confidenceLevel.level, "nino" -> nino.nino).toString)
        )
    )

  def authFailure(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader(WWW_AUTHENTICATE, """MDTP detail="BearerTokenExpired"""")
            .withHeader(ENROLMENT, "")
        )
    )

  def authRecordExistsWithoutNino(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(authorisationRequestJson, true, false))
        .willReturn(aResponse().withStatus(200).withBody(obj("confidenceLevel" -> L200.level).toString))
    )

  def accountsFound(
    nino:               Nino,
    confidenceLevel:    ConfidenceLevel = L200,
    credentialStrength: CredentialStrength = Strong,
    saUtr:              SaUtr = utr
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              obj(
                "confidenceLevel"    -> confidenceLevel.level,
                "nino"               -> nino.nino,
                "credentialStrength" -> credentialStrength.name,
                "saUtr"              -> saUtr.utr
              ).toString
            )
        )
    )

  def accountsFailure(): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader(WWW_AUTHENTICATE, """MDTP detail="BearerTokenExpired"""")
            .withHeader(ENROLMENT, "")
        )
    )

  def accountsFoundWithoutNino(
    confidenceLevel:    ConfidenceLevel    = L200,
    credentialStrength: CredentialStrength = Strong,
    saUtr:              SaUtr              = utr
  ): StubMapping =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              obj("confidenceLevel"    -> confidenceLevel.level,
                  "credentialStrength" -> credentialStrength.name,
                  "saUtr"              -> saUtr.utr).toString
            )
        )
    )
}
