package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import play.api.libs.json.Json.{stringify, toJson}
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status._
import uk.gov.hmrc.customerprofile.domain.{EmailPreference, OptInPage, PageType, Paperless, PaperlessOptOut, Preference, TermsAccepted, Version}
import uk.gov.hmrc.emailaddress.EmailAddress

object EntityResolverStub {

  private def entityDetailsByNino(
    nino:     String,
    entityId: String
  ): String = s"""
                 |{
                 |  "_id":"$entityId",
                 |  "sautr":"8040200778",
                 |  "nino":"$nino"
                 |}""".stripMargin

  private def preferences(
    optedIn: Boolean = true,
    email:   String  = "test@email.com",
    status:  Status  = Verified
  ): Preference =
    if (optedIn) {
      Preference(optedIn, Some(EmailPreference(EmailAddress(email), status)))
    } else Preference(digital = false)

  private def urlEqualToEntityResolverPaye(nino: String): UrlPattern =
    urlEqualTo(s"/entity-resolver/paye/$nino")

  def respondWithEntityDetailsByNino(
    nino:     String,
    entityId: String
  ): StubMapping =
    stubFor(
      get(urlEqualToEntityResolverPaye(nino))
        .willReturn(aResponse().withStatus(200).withBody(entityDetailsByNino(nino, entityId)))
    )

  def respondPreferencesWithPaperlessOptedIn(): StubMapping =
    stubFor(
      get(urlEqualToPreferences).willReturn(aResponse().withStatus(200).withBody(stringify(toJson(preferences()))))
    )

  def respondPreferencesWithBouncedEmail(): StubMapping =
    stubFor(
      get(urlEqualToPreferences)
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(stringify(toJson(preferences(status = Bounced))))
        )
    )

  def respondPreferencesNoPaperlessSet(): StubMapping =
    stubFor(
      get(urlEqualToPreferences)
        .willReturn(aResponse().withStatus(200).withBody(stringify(toJson(preferences(optedIn = false)))))
    )

  def respondNoPreferences(): StubMapping =
    stubFor(get(urlEqualToPreferences).willReturn(aResponse().withStatus(404)))

  def successPaperlessSettingsChange(): StubMapping =
    stubFor(post(urlEqualToPaperlessSettingsChange).willReturn(aResponse().withStatus(200)))

  def successPaperlessSettingsOptInWithVersion: StubMapping =
    stubFor(
      post(urlEqualToPaperlessSettingsChange)
        .withRequestBody(
          equalToJson(
            Json
              .toJson(
                Paperless(generic = TermsAccepted(Some(true), Some(OptInPage(Version(1, 1), 44, PageType.IosOptInPage))),
                          email   = EmailAddress("new-email@new-email.new.email"),
                          Some("en"))
              )
              .toString(),
            true,
            false
          )
        )
        .willReturn(aResponse().withStatus(200))
    )

  def successPaperlessSettingsOptOutWithVersion: StubMapping =
    stubFor(
      post(urlEqualToPaperlessSettingsChange)
        .withRequestBody(
          equalToJson(
            Json
              .toJson(
                PaperlessOptOut(generic = Some(TermsAccepted(Some(false), Some(OptInPage(Version(1, 1), 44, PageType.IosOptOutPage)))),
                  Some("en"))
              )
              .toString(),
            true,
            false
          )
        )
        .willReturn(aResponse().withStatus(200))
    )

  val urlEqualToPreferences:             UrlPattern = urlEqualTo(s"/preferences")
  val urlEqualToPaperlessSettingsChange: UrlPattern = urlEqualTo(s"/preferences/terms-and-conditions")
}
