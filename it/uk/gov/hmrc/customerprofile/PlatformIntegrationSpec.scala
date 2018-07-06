package uk.gov.hmrc.customerprofile

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlMatching, verify}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.api.controllers.DocumentationController
import uk.gov.hmrc.customerprofile.stubs.ServiceLocatorStub._
import uk.gov.hmrc.customerprofile.support.{BaseISpec, WireMockSupport, WiremockServiceLocatorSugar}
import uk.gov.hmrc.customerprofile.tasks.ServiceLocatorRegistrationTask


class PlatformIntegrationSpec extends BaseISpec with WiremockServiceLocatorSugar with WireMockSupport{
  val documentationController: DocumentationController = app.injector.instanceOf[DocumentationController]
  val request = FakeRequest()

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  "ServiceLocatorRegistrationTask" should {
    val task = app.injector.instanceOf[ServiceLocatorRegistrationTask]

    "register with the api platform" in {
      registrationWillSucceed()
      await(task.register) shouldBe true
      verify(1,
        postRequestedFor(urlMatching("/registration")).withHeader("content-type", equalTo("application/json")).
          withRequestBody(equalTo(regPayloadStringFor("customer-profile", "https://customer-profile.protected.mdtp")))
      )
    }

    "handle errors" in {
      registrationWillFail()
      await(task.register) shouldBe false
    }
  }

  "Documentation Controller" should {
    "provide definition with configurable whitelist" in {
      running(app) {

        val result = documentationController.definition()(request)
        status(result) shouldBe 200

        val definition = contentAsJson(result)
        (definition \\ "version").map(_.as[String]).head shouldBe "1.0"
      }
    }

    "provide RAML conf endpoint" in {
      running(app) {
        val result = documentationController.conf("1.0", "application.raml")(request)
        status(result) shouldBe 200
      }
    }
  }
}



