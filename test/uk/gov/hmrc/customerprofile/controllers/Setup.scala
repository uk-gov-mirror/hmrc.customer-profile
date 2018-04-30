/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.controllers

import java.util.UUID.randomUUID

import org.joda.time.DateTime.parse
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment, http}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.circuitbreaker.CircuitBreakerConfig
import uk.gov.hmrc.customerprofile.config.{ServicesCircuitBreaker, WSHttpImpl}
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action._
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.Pending
import uk.gov.hmrc.customerprofile.domain.NativeOS.Android
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.services._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TestEntityResolverConnector(preferencesStatus: PreferencesStatus, preference: Option[Preference], entity: Option[Entity] = None,
                                  serviceUrl: String, http: CoreGet with CorePost, runModeConfiguration: Configuration, environment: Environment)
  extends EntityResolverConnector(serviceUrl, http, runModeConfiguration, environment) with ServicesConfig with ServicesCircuitBreaker with Status {
  this: ServicesCircuitBreaker =>

  override protected def circuitBreakerConfig = CircuitBreakerConfig(serviceName = externalServiceName)

  override def paperlessSettings(paperless: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] = {
    Future.successful(preferencesStatus)
  }

  override def getPreferences()(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] = {
    Future.successful(preference)
  }

  override def getEntityIdByNino(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Entity] = Future(entity.get)
}

class TestAccountAccessControl(nino: Option[Nino],
                               ex: Option[Exception] = None,
                               authConnector: AuthConnector,
                               http: CoreGet,
                               authUrl: String,
                               serviceConfidenceLevel: Int) extends AccountAccessControl(authConnector, http, authUrl, serviceConfidenceLevel) {
  override def grantAccess(taxId: Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    ex match {
      case None => Future(Unit)
      case Some(failure) => Future.failed(failure)
    }
  }

  override def accounts(implicit hc: HeaderCarrier) = Future(Accounts(nino, None, routeToIV = false, routeToTwoFactor = false, "102030394AAA"))
}

trait Setup extends MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val journeyId: String = randomUUID().toString
  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val paperlessJsonBody: JsValue = Json.toJson(Paperless(TermsAccepted(true), EmailAddress("a@b.com")))
  val noNinoOnAccount: JsValue = Json.parse("""{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}""")
  val lowCL: JsValue = Json.parse("""{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}""")
  val paperlessRequestNoAccept: FakeRequest[JsValue] = FakeRequest().withBody(paperlessJsonBody).withHeaders("Content-Type" -> "application/json")
  val paperlessRequest: FakeRequest[JsValue] = FakeRequest().withBody(paperlessJsonBody)
    .withHeaders(http.HeaderNames.CONTENT_TYPE → MimeTypes.JSON, http.HeaderNames.ACCEPT → "application/vnd.hmrc.1.0+json")

  val nino = Nino("CS700100A")
  val testAccount = Accounts(Some(nino), None, routeToIV = false, routeToTwoFactor = false, "102030394AAA")

  val person =
    PersonDetails(
      "etag",
      Person(Some("Jennifer"), None, Some("Thorsteinson"), None, Some("Ms"), None, Some("Female"), Some(parse("1999-01-31")), Some(nino)),
      Some(Address(Some("999 Big Street"), Some("Worthing"), Some("West Sussex"), None, None, Some("BN99 8IG"), None, None, None)))

  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(POST, "url").withBody(body)
    .withHeaders("Content-Type" -> "application/json")

  val upgradeFalse: JsValue = Json.parse("""{"upgrade":false}""")
  val upgradeTrue: JsValue = Json.parse("""{"upgrade":true}""")
  val unknownOS: JsValue = Json.parse("""{"obj.os":[{"msg":["unknown os"],"args":[]}]}""")

  val preferenceSuccess: JsValue = Json.parse("""{"digital":true,"email":{"email":"name@email.co.uk","status":"verified"}}""")

  val deviceVersion = DeviceVersion(Android, "1.0.1")
  lazy val jsonDeviceVersionRequest: FakeRequest[JsValue] = fakeRequest(Json.toJson(deviceVersion)).withHeaders(acceptHeader)
  lazy val jsonUnknownDeviceOSRequest: FakeRequest[JsValue] = fakeRequest(Json.parse("""{"os":"unicorn","version":"1.2.3"}""")).withHeaders(acceptHeader)

  lazy val defaultPreference = Some(Preference(digital = true, Some(EmailPreference(EmailAddress("someone@something.com"), Pending))))
  lazy val defaultEntity = Some(Entity(_id = "3333333333333333333"))

  def upgradeService(status: Boolean) = new TestUpgradeRequiredCheckerService(status)

  class TestUpgradeRequiredCheckerService(state: Boolean) extends UpgradeRequiredCheckerService {
    override def upgradeRequired(deviceVersion: DeviceVersion)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] = Future.successful(state)
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockPreferencesConnector: PreferencesConnector = mock[PreferencesConnector]
  val mockLiveCustomerProfileService: LiveCustomerProfileService = mock[LiveCustomerProfileService]
  val mockEntityResolverConnector: EntityResolverConnector = mock[EntityResolverConnector]
  val mockHttp: WSHttpImpl = mock[WSHttpImpl]
  val mockConfiguration: Configuration = mock[Configuration]
  val mockEnvironment: Environment = mock[Environment]

  lazy val testAccess = new TestAccountAccessControl(Some(nino), None, mockAuthConnector,
    mockHttp, "someUrl", 200)

  lazy val testSandboxCustomerProfileService: SandboxCustomerProfileService = new SandboxCustomerProfileService()
  lazy val sandboxCompositeAction: AccountAccessControlCheckOff = new AccountAccessControlCheckOff(testAccess)

  lazy val testCustomerProfileService = new LiveCustomerProfileService(mockCitizenDetailsConnector, mockPreferencesConnector,
    mockEntityResolverConnector, testAccess, mockConfiguration, mockAuditConnector) {
    var saveDetails: Map[String, String] = Map.empty

    override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
      saveDetails = details
      Future.successful(AuditResult.Success)
    }
  }

  lazy val testCompositeAction = new AccountAccessControlWithHeaderCheck(testAccess)
  lazy val testCustomerProfileController = new LiveCustomerProfileController(mockLiveCustomerProfileService, testCompositeAction)

}

trait AuthorityTest extends UnitSpec {
  self: Setup =>

  def testNoNINO(func: => play.api.mvc.Result): Assertion = {
    val result: play.api.mvc.Result = func

    status(result) shouldBe 401
    contentAsJson(result) shouldBe noNinoOnAccount
  }

  def testLowCL(func: => play.api.mvc.Result): Assertion = {
    val result: play.api.mvc.Result = func

    status(result) shouldBe 401
    contentAsJson(result) shouldBe lowCL
  }

}

trait Success extends Setup {
  val controller = new CustomerProfileController {
    val app = "Success Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait PaperlessCreated extends Setup {
  lazy val testEntityResolverConnector = new TestEntityResolverConnector(PreferencesCreated, Some(Preference(digital = false)), defaultEntity,
    "someUrl", mockHttp, mockConfiguration, mockEnvironment)

  override lazy val testCustomerProfileService = new LiveCustomerProfileService(mockCitizenDetailsConnector, mockPreferencesConnector,
    testEntityResolverConnector, testAccess, mockConfiguration, mockAuditConnector) {
    var saveDetails: Map[String, String] = Map.empty

    override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
      saveDetails = details
      Future.successful(AuditResult.Success)
    }
  }

  val controller = new CustomerProfileController {
    val app = "Success Paperless Created"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait PaperlessAlreadyOptedIn extends Setup {
  lazy val testEntityResolverConnector = new TestEntityResolverConnector(PreferencesExists, Some(Preference(digital = false)), defaultEntity,
    "someUrl", mockHttp, mockConfiguration, mockEnvironment)

  override lazy val testCustomerProfileService = new LiveCustomerProfileService(mockCitizenDetailsConnector, mockPreferencesConnector,
    testEntityResolverConnector, testAccess, mockConfiguration, mockAuditConnector) {
    var saveDetails: Map[String, String] = Map.empty

    override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
      saveDetails = details
      Future.successful(AuditResult.Success)
    }
  }

  val controller = new CustomerProfileController {
    val app = "Success Paperless Already Opted In"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait AccessCheck extends Setup {
  override lazy val testAccess = new TestAccountAccessControl(None, Some(new FailToMatchTaxIdOnAuth("controlled explosion")),
    mockAuthConnector, mockHttp, "someUrl", 200)

  val controller = new CustomerProfileController {
    override val app: String = "Access Check"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait PreferenceConflict extends Setup {
  lazy val testEntityResolverConnector = new TestEntityResolverConnector(EmailNotExist, defaultPreference, defaultEntity,
    "someUrl", mockHttp, mockConfiguration, mockEnvironment)

  override lazy val testCustomerProfileService = new LiveCustomerProfileService(mockCitizenDetailsConnector, mockPreferencesConnector,
    testEntityResolverConnector, testAccess, mockConfiguration, mockAuditConnector) {
    var saveDetails: Map[String, String] = Map.empty

    override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
      saveDetails = details
      Future.successful(AuditResult.Success)
    }
  }

  val controller = new CustomerProfileController {
    val app = "Preference conflict"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait AuthWithoutNino extends Setup with AuthorityTest {

  override lazy val testAccess = new TestAccountAccessControl(None, None, mockAuthConnector,
    mockHttp, "someUrl", 200) {
    lazy val exception = new NinoNotFoundOnAccount("The user must have a National Insurance Number")

    override def accounts(implicit hc: HeaderCarrier): Future[Accounts] = Future.failed(exception)

    override def grantAccess(taxId: Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
  }

  val controller = new CustomerProfileController {
    override val app = "AuthWithoutNino Customer Profile"
    override val service = testCustomerProfileService
    override val accessControl = testCompositeAction
  }
}

trait AuthWithLowCL extends Setup with AuthorityTest {
  val routeToIv = true
  val routeToTwoFactor = false

  override lazy val testAccess = new TestAccountAccessControl(None, None, mockAuthConnector,
    mockHttp, "someUrl", 200) {
    lazy val exception = new AccountWithLowCL("Forbidden to access since low CL")

    override def accounts(implicit hc: HeaderCarrier): Future[Accounts] =
      Future.successful(Accounts(Some(nino), None, routeToIv, routeToTwoFactor, "102030394AAA"))

    override def grantAccess(taxId: Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
  }

  val controller = new CustomerProfileController {
    val app = "AuthWithLowCL Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }

}

trait SandboxSuccess extends Setup {
  val controller = new CustomerProfileController {
    val app = "Sandbox Customer Profile"
    override val service: CustomerProfileService = testSandboxCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = sandboxCompositeAction
  }
}

trait SandboxPaperlessCreated extends SandboxSuccess {
  lazy val entityConnector = new TestEntityResolverConnector(PreferencesCreated, defaultPreference, defaultEntity,
    "someUrl", mockHttp, mockConfiguration, mockEnvironment)

  override lazy val testCustomerProfileService = new LiveCustomerProfileService(mockCitizenDetailsConnector, mockPreferencesConnector,
    entityConnector, testAccess, mockConfiguration, mockAuditConnector) {
    var saveDetails: Map[String, String] = Map.empty

    override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
      saveDetails = details
      Future.successful(AuditResult.Success)
    }
  }

  override val controller = new CustomerProfileController {
    val app = "SandboxPaperlessCreated Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait SandboxPaperlessFailed extends SandboxPaperlessCreated {
  override lazy val entityConnector = new TestEntityResolverConnector(PreferencesFailure, defaultPreference, defaultEntity,
    "someUrl", mockHttp, mockConfiguration, mockEnvironment)

  override lazy val testCustomerProfileService = new LiveCustomerProfileService(mockCitizenDetailsConnector, mockPreferencesConnector,
    entityConnector, testAccess, mockConfiguration, mockAuditConnector) {
    var saveDetails: Map[String, String] = Map.empty

    override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
      saveDetails = details
      Future.successful(AuditResult.Success)
    }
  }

  override val controller = new CustomerProfileController {
    val app = "SandboxPaperlessFailed Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait SuccessNativeVersionChecker extends Setup {
  lazy val upgrade = false

  val controller = new NativeVersionCheckerController {
    val app = "Test-Native-Version-Checker"
    override val upgradeRequiredCheckerService: TestUpgradeRequiredCheckerService = upgradeService(upgrade)
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}