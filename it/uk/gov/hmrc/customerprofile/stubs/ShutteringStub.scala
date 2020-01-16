package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object ShutteringStub {

  def stubForShutteringDisabled: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/customer-profile/shuttered-status?journeyId=b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": false,
                       |  "title":     "",
                       |  "message":    ""
                       |}
          """.stripMargin)
      )
    )

  def stubForShutteringEnabled: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/customer-profile/shuttered-status?journeyId=b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": true,
                       |  "title":     "Shuttered",
                       |  "message":   "Preferences are currently not available"
                       |}
          """.stripMargin)
      )
    )


}
