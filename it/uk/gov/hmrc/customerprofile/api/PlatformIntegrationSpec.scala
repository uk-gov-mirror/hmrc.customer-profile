package uk.gov.hmrc.customerprofile.api

import org.scalatest.concurrent.Eventually
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws.WSResponse
import play.api.test.PlayRunners
import uk.gov.hmrc.customerprofile.support.BaseISpec

/**
  * Testcase to verify the capability of integration with the API platform.
  *
  * 1a, To expose API's to Third Party Developers, the service needs to define the APIs in a definition.json and make it available under api/definition GET endpoint
  * 1b, For all of the endpoints defined in the definition.json a documentation.xml needs to be provided and be available under api/documentation/[version]/[endpoint name] GET endpoint
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
      "api.access.white-list.applicationIds" -> Seq(appId1, appId2)
    )
  )

  "microservice" should {
    "provide definition with configurable whitelist" in {
      val result: WSResponse = await(wsUrl("/api/definition").get())
      result.status shouldBe 200

      val definition: JsValue      = result.json
      val versions:   Seq[JsValue] = (definition \ "api" \\ "versions").head.as[JsArray].value
      versions.length shouldBe 1

      val versionJson: JsValue = versions.head
      (versionJson \ "version").as[String] shouldBe "1.0"

      val accessDetails: JsValue = (versionJson \\ "access").head
      (accessDetails \ "type").as[String]                           shouldBe "PRIVATE"
      (accessDetails \ "whitelistedApplicationIds").head.as[String] shouldBe appId1
      (accessDetails \ "whitelistedApplicationIds")(1).as[String]   shouldBe appId2
    }

    "provide RAML conf endpoint" in {
      val result: WSResponse = await(wsUrl("/api/conf/1.0/application.raml").get())
      result.status shouldBe 200
    }
  }
}
