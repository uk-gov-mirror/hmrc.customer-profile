package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object PreferencesStub {

  def successfulPendingEmailUpdate(entityId: String) =
    stubFor(put(urlEqualToPreferencesPendingEmail(entityId))
      .willReturn(aResponse()
        .withStatus(200)))

  def conflictPendingEmailUpdate(entityId: String) =
    stubFor(put(urlEqualToPreferencesPendingEmail(entityId))
      .willReturn(aResponse()
        .withStatus(409)))

  def notFoundPendingEmailUpdate(entityId: String) =
    stubFor(put(urlEqualToPreferencesPendingEmail(entityId))
      .willReturn(aResponse()
        .withStatus(404)))

  def errorPendingEmailUpdate(entityId: String) =
    stubFor(put(urlEqualToPreferencesPendingEmail(entityId))
      .willReturn(aResponse()
        .withStatus(500)))

  private def urlEqualToPreferencesPendingEmail(entityId: String) = {
    urlEqualTo(s"/preferences/$entityId/pending-email")
  }

}
