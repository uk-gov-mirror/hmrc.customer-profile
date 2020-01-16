package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object PreferencesStub {

  def successfulPendingEmailUpdate(entityId: String): StubMapping =
    stubFor(
      put(urlEqualToPreferencesPendingEmail(entityId))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

  def conflictPendingEmailUpdate(entityId: String): StubMapping =
    stubFor(
      put(urlEqualToPreferencesPendingEmail(entityId))
        .willReturn(
          aResponse()
            .withStatus(409)
        )
    )

  def notFoundPendingEmailUpdate(entityId: String): StubMapping =
    stubFor(
      put(urlEqualToPreferencesPendingEmail(entityId))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )

  def errorPendingEmailUpdate(entityId: String): StubMapping =
    stubFor(
      put(urlEqualToPreferencesPendingEmail(entityId))
        .willReturn(
          aResponse()
            .withStatus(500)
        )
    )

  private def urlEqualToPreferencesPendingEmail(entityId: String): UrlPattern =
    urlEqualTo(s"/preferences/$entityId/pending-email")

}
