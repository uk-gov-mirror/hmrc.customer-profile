package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object ServiceLocatorStub {

  def registrationWillSucceed(): Unit = {
    stubFor(post(urlEqualTo("/registration"))
      .willReturn(aResponse()
        .withStatus(204)))
  }

  def registrationWillFail(): Unit = {
    stubFor(post(urlEqualTo("/registration"))
      .willReturn(aResponse()
        .withStatus(500)))
  }
}