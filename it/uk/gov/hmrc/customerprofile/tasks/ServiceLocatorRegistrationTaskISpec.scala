package uk.gov.hmrc.customerprofile.tasks

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlMatching, verify}
import uk.gov.hmrc.customerprofile.stubs.ServiceLocatorStub._
import uk.gov.hmrc.customerprofile.support.{BaseISpec, WiremockServiceLocatorSugar}

class ServiceLocatorRegistrationTaskISpec extends BaseISpec with WiremockServiceLocatorSugar {
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
}
