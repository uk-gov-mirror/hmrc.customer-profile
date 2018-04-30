package uk.gov.hmrc.customerprofile.tasks

/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Span}
import play.api.Mode.{Dev, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, PlayRunners}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.api.config.ServiceLocatorConfig
import uk.gov.hmrc.api.connector.ServiceLocatorConnector
import uk.gov.hmrc.api.controllers.DocumentationController
import uk.gov.hmrc.customerprofile.support.BaseISpec
import uk.gov.hmrc.http.{CorePost, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.customerprofile.utils.WiremockServiceLocatorSugar

import scala.concurrent.Future

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
class PlatformIntegrationSpec extends BaseISpec with Eventually with WiremockServiceLocatorSugar with ScalaFutures with PlayRunners {
  lazy val testApiServiceLocatorConnector = new TestApiServiceLocatorConnector(null, null, null)

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[ServiceLocatorConnector]).toInstance(testApiServiceLocatorConnector)
    }
  }

  trait Setup {
    val documentationController = DocumentationController
    val request = FakeRequest()

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
  }

  override protected def appBuilder: GuiceApplicationBuilder = super.appBuilder.configure(
    "appName" -> "application-name",
    "appUrl" -> "http://microservice-name.service",
    "microservice.services.service-locator.host" -> wireMockHost,
    "microservice.services.service-locator.port" -> wireMockPort
  ).overrides(new TestGuiceModule)

  "microservice" should {
    "register itself with the api platform automatically at start up" in {
      val connector = app.injector.instanceOf[ServiceLocatorConnector].asInstanceOf[TestApiServiceLocatorConnector]
      connector.regsisteredSuccessfully shouldBe false

      running(app) {
        eventually(Timeout(Span(1000 * 20, Millis))) {
          connector.regsisteredSuccessfully shouldBe true
        }
      }
    }

    "provide definition endpoint and documentation endpoints for each api" in new Setup {
      running(app) {
        () ⇒ {

          def verifyDocumentationPresent(version: String, endpointName: String) {
            withClue(s"Getting documentation version '$version' of endpoint '$endpointName'") {
              val documentationResult = documentationController.documentation(version, endpointName)(request)
              status(documentationResult) shouldBe 200
            }
          }

          val result = documentationController.definition()(request)
          status(result) shouldBe 200

          val jsonResponse = jsonBodyOf(result).futureValue
          val versions: Seq[String] = (jsonResponse \\ "version") map (_.as[String])
          val endpointNames: Seq[Seq[String]] = (jsonResponse \\ "endpoints").map(_ \\ "endpointName").map(_.map(_.as[String]))

          versions.zip(endpointNames).flatMap {
            case (version, endpoint) => endpoint.map(endpointName ⇒ (version, endpointName))
          } foreach {
            case (version, endpointName) =>
              println(s" Verifying version $version and endpoint name $endpointName.")
              verifyDocumentationPresent(version, endpointName)
          }
        }
      }
    }

    "provide RAML conf endpoint" in new Setup {
      running(app) {
        () ⇒ {
          val result = documentationController.conf("1.0", "application.raml")(request)
          status(result) shouldBe 200
        }
      }
    }
  }
}

class TestApiServiceLocatorConnector(override val runModeConfiguration: Configuration, environment: Environment, wsHttp: WSHttp)
  extends ServiceLocatorConnector with ServiceLocatorConfig with AppName {

  var regsisteredSuccessfully: Boolean = false

  override def register(implicit hc: HeaderCarrier): Future[Boolean] = {
    regsisteredSuccessfully = true
    Future successful true
  }

  override val appUrl: String = "test"
  override val serviceUrl: String = "test"
  override val handlerOK: () => Unit = () => ()
  override val handlerError: Throwable => Unit = _ => ()
  override val metadata: Option[Map[String, String]] = None
  override val http: CorePost = wsHttp

  override def configuration: Configuration = runModeConfiguration

  override protected def mode: Mode = Dev
}

