package uk.gov.hmrc.customerprofile.api

import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Millis, Span}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws.WSResponse
import play.api.test.PlayRunners
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.customerprofile.stubs.ServiceLocatorStub._

/**
  * Testcase to verify the capability of integration with the API platform.
  *
  * 1, To integrate with API platform the service needs to register itself to the service locator by calling the /registration endpoint and providing
  * - application name
  * - application url
  *
  * 2a, To expose API's to Third Party Developers, the service needs to define the APIs in a definition.json and make it available under api/definition GET endpoint
  * 2b, For all of the endpoints defined in the definition.json a documentation.xml needs to be provided and be available under api/documentation/[version]/[endpoint name] GET endpoint
  * Example: api/documentation/1.0/Fetch-Some-Data
  *
  * See: confluence ApiPlatform/API+Platform+Architecture+with+Flows
  */
class PlatformIntegrationSpec extends BaseISpec with Eventually with PlayRunners {

  private val appId1: String = "00010002-0003-0004-0005-000600070008"
  private val appId2: String = "00090002-0003-0004-0005-000600070008"

  override protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    config ++
      Map(
        "microservice.services.service-locator.host" -> wireMockHost,
        "microservice.services.service-locator.port" -> wireMockPort,
        "api.access.white-list.applicationIds" -> Seq(appId1, appId2)
      )
  )

  "microservice" should {
    "register itself with the api platform automatically at start up" in {
      registerShouldNotHaveBeenCalled()

      eventually(Timeout(Span(1000 * 20, Millis))) {
        registerShouldHaveBeenCalled()
      }
    }

    "provide definition with configurable whitelist" in {
      val result: WSResponse = await(wsUrl("/api/definition").get())
      result.status shouldBe 200

      val definition: JsValue = result.json
      val versions: Seq[JsValue] = (definition \ "api" \\ "versions").head.as[JsArray].value
      versions.length shouldBe 1

      val versionJson: JsValue = versions.head
      (versionJson \ "version").as[String] shouldBe "1.0"

      val accessDetails: JsValue = (versionJson \\ "access").head
      (accessDetails \ "type").as[String] shouldBe "PRIVATE"
      (accessDetails \ "whitelistedApplicationIds").head.as[String] shouldBe appId1
      (accessDetails \ "whitelistedApplicationIds") (1).as[String] shouldBe appId2
    }

    "provide RAML conf endpoint" in {
      val result: WSResponse = await(wsUrl("/api/conf/1.0/application.raml").get())
      result.status shouldBe 200
    }
  }
}
