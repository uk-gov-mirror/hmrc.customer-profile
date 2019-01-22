package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.api.domain.Registration

object ServiceLocatorStub {

  def registrationWillSucceed(): StubMapping =
    stubFor(
      post(urlEqualTo("/registration"))
        .willReturn(aResponse()
          .withStatus(204)))

  def registrationWillFail(): StubMapping =
    stubFor(
      post(urlEqualTo("/registration"))
        .willReturn(aResponse()
          .withStatus(500)))

  private def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  private val registrationPattern: RequestPatternBuilder = postRequestedFor(urlPathEqualTo("/registration"))
    .withHeader("content-type", equalTo("application/json"))
    .withRequestBody(equalTo(regPayloadStringFor("customer-profile", "https://customer-profile.protected.mdtp")))

  def registerShouldHaveBeenCalled(): Unit = verify(1, registrationPattern)

  def registerShouldNotHaveBeenCalled(): Unit = verify(0, registrationPattern)
}
