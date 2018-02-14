package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status._
import uk.gov.hmrc.customerprofile.domain.{EmailPreference, Preference}
import uk.gov.hmrc.emailaddress.EmailAddress

object EntityResolverStub {

  private def entityDetailsByNino(nino: String, entityId: String) = s"""
                                       |{
                                       |  "_id":"$entityId",
                                       |  "sautr":"8040200778",
                                       |  "nino":"$nino"
                                       |}""".stripMargin

  private def preferences(optedIn: Boolean = true, email: String = "test@email.com", status: Status = Verified): Preference = {
    if(optedIn) {
      Preference(optedIn, Some(EmailPreference(EmailAddress(email), status)))
    }
    else Preference(false)
  }

  private def urlEqualToEntityResolverPaye(nino: String) = {
    urlEqualTo(s"/entity-resolver/paye/${nino}")
  }

  def respondWithEntityDetailsByNino(nino: String, entityId: String) =
  stubFor(get(urlEqualToEntityResolverPaye(nino))
    .willReturn(aResponse()
    .withStatus(200)
    .withBody(entityDetailsByNino(nino, entityId))))

  def respondPreferencesWithPaperlessOptedIn() = {
    stubFor(get(urlEqualToPreferences)
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.stringify(Json.toJson(preferences())))))
  }

  def respondPreferencesWithBouncedEmail() = {
    stubFor(get(urlEqualToPreferences)
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.stringify(Json.toJson(preferences(true, status = Bounced))))))
  }

  def respondPreferencesNoPaperlessSet() = {
    stubFor(get(urlEqualToPreferences)
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.stringify(Json.toJson(preferences(false))))))
  }

  def respondNoPreferences() = {
    stubFor(get(urlEqualToPreferences)
      .willReturn(aResponse()
        .withStatus(404)))
  }

  def successPaperlessSettingsOptIn() = {
    stubFor(post(urlEqualToPaperlessSettingsOptIn)
      .willReturn(aResponse()
        .withStatus(200)))
  }

  val urlEqualToPreferences = urlEqualTo(s"/preferences")
  val urlEqualToPaperlessSettingsOptIn = urlEqualTo(s"/preferences/terms-and-conditions")

}
