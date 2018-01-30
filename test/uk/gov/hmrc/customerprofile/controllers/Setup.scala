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

import java.util.UUID

import com.typesafe.config.Config
import org.apache.http.HttpHeaders
import org.joda.time.DateTime.parse
import org.slf4j.Logger
import play.api.LoggerLike
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.circuitbreaker.CircuitBreakerConfig
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControl, AccountAccessControlCheckOff, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.customerprofile.domain.NativeOS.Android
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.services._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


class TestCitizenDetailsConnector(httpResponse: Future[HttpResponse]) extends CitizenDetailsConnector {
  override lazy val citizenDetailsConnectorUrl = "someUrl"
  override lazy val http: CoreGet = new CoreGet with HttpGet {
    override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = httpResponse

    override val hooks: Seq[HttpHook] = NoneRequired

    override def configuration: Option[Config] = None
  }
}

class TestEntityResolverConnector(preferencesStatus: PreferencesStatus, preference:Option[Preference], entity: Option[Entity] = None) extends EntityResolverConnector with ServicesConfig with ServicesCircuitBreaker with play.api.http.Status {
  this: ServicesCircuitBreaker =>

  override protected def circuitBreakerConfig = CircuitBreakerConfig(serviceName = externalServiceName)

  override def http: CoreGet with CorePost with CorePut = ???

  override def serviceUrl: String = ???

  override def paperlessSettings(paperless: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] = {
    Future.successful(preferencesStatus)
  }

  override def getPreferences()(implicit headerCarrier: HeaderCarrier, ex : ExecutionContext): Future[Option[Preference]] = {
    Future.successful(preference)
  }

  override def getEntityIdByNino(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Entity] = Future(entity.get)
}

class TestAuthConnector(nino: Option[Nino], ex:Option[Exception]=None) extends AuthConnector {
  override val serviceUrl: String = "someUrl"

  override def serviceConfidenceLevel: ConfidenceLevel = ???

  override def http: CoreGet = ???

  override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future(Accounts(nino, None, false, false, "102030394AAA"))

  override def grantAccess(taxId:Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    ex match {
      case None => Future(Unit)
      case Some(failure) => Future.failed(failure)
    }
  }
}

class TestPreferencesConnector(httpResponse: HttpResponse) extends PreferencesConnector with ServicesConfig with ServicesCircuitBreaker {
  override def serviceUrl: String = "http://preferences.service"

  override def http: HttpPut = new HttpPut {
    override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = Future(httpResponse)

    override def configuration: Option[Config] = None

    override val hooks: Seq[HttpHook] = Seq.empty
  }

  override protected val externalServiceName: String = "preferences"
}

class TestCustomerProfileService(testCDConnector: CitizenDetailsConnector,
                                 testAuthConnector: TestAuthConnector,
                                 testEntityResolver: EntityResolverConnector,
                                 testAuditConnector: AuditConnector,
                                 testPreferencesConnector: PreferencesConnector) extends LiveCustomerProfileService {

  override lazy val appName = "TestCustomerProfileService"
  val citizenDetailsConnector = testCDConnector
  val authConnector = testAuthConnector
  val entityResolver = testEntityResolver
  val auditConnector: AuditConnector = testAuditConnector
  val preferencesConnector: PreferencesConnector = testPreferencesConnector
}

class TestAccessCheck(testAuthConnector: TestAuthConnector) extends AccountAccessControl {
  override val authConnector: AuthConnector = testAuthConnector
}

class TestAccountAccessControlWithAccept(testAccessCheck: AccountAccessControl) extends AccountAccessControlWithHeaderCheck {
  override val accessControl: AccountAccessControl = testAccessCheck
}

trait Setup {
  implicit val hc = HeaderCarrier()

  val journeyId = UUID.randomUUID().toString
  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val paperlessJsonBody = Json.toJson(Paperless(TermsAccepted(true), EmailAddress("a@b.com")))
  val noNinoOnAccount = Json.parse("""{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}""")
  val lowCL = Json.parse("""{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}""")
  val weakCredStrength = Json.parse("""{"code":"WEAK_CRED_STRENGTH","message":"Credential Strength on account does not allow access"}""")
  val paperlessRequestNoAccept = FakeRequest().withBody(paperlessJsonBody).withHeaders("Content-Type" -> "application/json")
  val paperlessRequest = FakeRequest().withBody(paperlessJsonBody)
    .withHeaders(play.api.http.HeaderNames.CONTENT_TYPE → MimeTypes.JSON, play.api.http.HeaderNames.ACCEPT → "application/vnd.hmrc.1.0+json")
  val changeEmail = Json.obj("email" → "email@new-email.email")
  val changeEmailRequest = FakeRequest().withBody(changeEmail)
    .withHeaders(play.api.http.HeaderNames.CONTENT_TYPE → MimeTypes.JSON, play.api.http.HeaderNames.ACCEPT → "application/vnd.hmrc.1.0+json")
  val changeEmailRequestNoAccept = FakeRequest().withBody(changeEmail)
    .withHeaders(play.api.http.HeaderNames.CONTENT_TYPE → MimeTypes.JSON)

  val nino = Nino("CS700100A")
  val testAccount = Accounts(Some(nino), None, false, false,"102030394AAA")

  val person =
    PersonDetails(
      "etag",
      Person(Some("Jennifer"), None, Some("Thorsteinson"), None, Some("Ms"), None, Some("Female"), Some(parse("1999-01-31")), Some(nino)),
      Some(Address(Some("999 Big Street"), Some("Worthing"), Some("West Sussex"), None, None, Some("BN99 8IG"), None, None, None )))

  val personWithMissingData = PersonDetails("etag", Person(firstName = Some(""), Some("Theo"), lastName = None,
    Some("LM"), Some("Mr"), None, Some("Male"), dateOfBirth = None, nino = None), None)

  val acceptHeader = "Accept" -> "application/vnd.hmrc.1.0+json"
  def fakeRequest(body:JsValue) = FakeRequest(POST, "url").withBody(body)
    .withHeaders("Content-Type" -> "application/json")

  val upgradeFalse = Json.parse("""{"upgrade":false}""")
  val upgradeTrue = Json.parse("""{"upgrade":true}""")
  val unknownOS = Json.parse("""{"obj.os":[{"msg":["unknown os"],"args":[]}]}""")

  val preferenceSuccess = Json.parse("""{"digital":true,"email":{"email":"name@email.co.uk","status":"verified"}}""")

  val deviceVersion = DeviceVersion(Android, "1.0.1")
  lazy val jsonDeviceVersionRequest = fakeRequest(Json.toJson(deviceVersion)).withHeaders(acceptHeader)
  lazy val jsonUnknownDeviceOSRequest = fakeRequest(Json.parse("""{"os":"unicorn","version":"1.2.3"}""")).withHeaders(acceptHeader)

  lazy val http200ResponseCid = Future.successful(HttpResponse(200, Some(Json.toJson(person))))
  lazy val missingDataHttp200ResponseCid = Future.successful(HttpResponse(200, Some(Json.toJson(personWithMissingData))))

  lazy val authConnector = new TestAuthConnector(Some(nino))
  lazy val cdConnector = new TestCitizenDetailsConnector(http200ResponseCid)
  lazy val missingDataConnector = new TestCitizenDetailsConnector(missingDataHttp200ResponseCid)

  lazy val defaultPreference = Some(Preference(true, Some(EmailPreference(EmailAddress("someone@something.com"), EmailPreference.Status.Pending))))
  lazy val defaultEntity = Some(Entity(_id = "3333333333333333333"))
  lazy val entityConnector = new TestEntityResolverConnector(PreferencesExists, defaultPreference, defaultEntity)
  lazy val prefererencesConnector = new TestPreferencesConnector(HttpResponse(OK))
  lazy val conflictPrefererencesConnector = new TestPreferencesConnector(HttpResponse(CONFLICT))
  lazy val notFoundPrefererencesConnector = new TestPreferencesConnector(HttpResponse(NOT_FOUND))
  lazy val errorPrefererencesConnector = new TestPreferencesConnector(HttpResponse(INTERNAL_SERVER_ERROR))
  lazy val testAccess = new TestAccessCheck(authConnector)
  lazy val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  object MicroserviceAuditConnectorTest extends AuditConnector with RunMode {
    override def auditingConfig: AuditingConfig = AuditingConfig(None, enabled = false)
  }

  lazy val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, prefererencesConnector)
  lazy val missingDataCustomerProfileService = new TestCustomerProfileService(missingDataConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, prefererencesConnector)
  lazy val conflictPreferenceCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, conflictPrefererencesConnector)
  lazy val notFoundPreferenceCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, notFoundPrefererencesConnector)
  lazy val errorPreferenceCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, errorPrefererencesConnector)

  lazy val testSandboxCustomerProfileService = SandboxCustomerProfileService
  lazy val sandboxCompositeAction = AccountAccessControlCheckOff

  def upgradeService(status:Boolean) = new TestUpgradeRequiredCheckerService(status)

  class TestUpgradeRequiredCheckerService(state:Boolean) extends UpgradeRequiredCheckerService {
    override def upgradeRequired(deviceVersion: DeviceVersion)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] = Future.successful(state)
  }

}

trait AuthorityTest extends UnitSpec {
  self: Setup =>

  def testNoNINO(func: => play.api.mvc.Result) = {
    val result: play.api.mvc.Result = func

    status(result) shouldBe 401
    contentAsJson(result) shouldBe noNinoOnAccount
  }

  def testLowCL(func: => play.api.mvc.Result) = {
    val result: play.api.mvc.Result = func

    status(result) shouldBe 401
    contentAsJson(result) shouldBe lowCL
  }

  def testWeakCredStrength(func: => play.api.mvc.Result) = {
    val result: play.api.mvc.Result = func

    status(result) shouldBe 401
    contentAsJson(result) shouldBe weakCredStrength
  }
}

trait Success extends Setup {
  val controller = new CustomerProfileController {
    val app = "Success Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait MissingDataSuccess extends Setup {
  val stubbedLogger = new LoggerLikeStub()

  val controller = new CustomerProfileController {
    val app = "Success Customer Profile"
    override val service: CustomerProfileService = missingDataCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction

    override def getLogger = stubbedLogger
  }
}

trait AccessCheck extends Setup {
  override lazy val authConnector = new TestAuthConnector(None, Some(new FailToMatchTaxIdOnAuth("controlled explosion")))
  override lazy val testAccess = new TestAccessCheck(authConnector)
  override lazy val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new CustomerProfileController {
    override val app: String = "Access Check"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait PreferenceNotFound extends Setup {
  val controller = new CustomerProfileController {
    val app = "Preference Not Found"
    val entityConnectorLocal = new TestEntityResolverConnector(PreferencesExists, None, defaultEntity)
    lazy val testCustomerProfileServiceLocal = new TestCustomerProfileService(cdConnector, authConnector, entityConnectorLocal, MicroserviceAuditConnectorTest, notFoundPrefererencesConnector)
    override val service: CustomerProfileService = testCustomerProfileServiceLocal
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait PreferenceConflict extends Setup {
  val controller = new CustomerProfileController {
    val app = "Preference conflict"
    val entityConnectorLocal = new TestEntityResolverConnector(PreferencesExists, None, defaultEntity)
    lazy val testCustomerProfileServiceLocal = new TestCustomerProfileService(cdConnector, authConnector, entityConnectorLocal, MicroserviceAuditConnectorTest, conflictPrefererencesConnector)
    override val service: CustomerProfileService = testCustomerProfileServiceLocal
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait AuthWithoutNino extends Setup with AuthorityTest {

  override lazy val authConnector = new TestAuthConnector(None) {
    lazy val exception = new NinoNotFoundOnAccount("The user must have a National Insurance Number")
    override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future.failed(exception)
    override def grantAccess(taxId:Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
  }

  override lazy val testAccess = new TestAccessCheck(authConnector)
  override lazy val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new CustomerProfileController {
    val app = "AuthWithoutNino Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait AuthWithLowCL extends Setup with AuthorityTest {
  val routeToIv=true
  val routeToTwoFactor=false

  override lazy val authConnector = new TestAuthConnector(None) {
    lazy val exception = new AccountWithLowCL("Forbidden to access since low CL")
    override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future.successful(Accounts(Some(nino), None, routeToIv, routeToTwoFactor, "102030394AAA"))
    override def grantAccess(taxId:Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
  }

  override lazy val testAccess = new TestAccessCheck(authConnector)
  override lazy val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new CustomerProfileController {
    val app = "AuthWithLowCL Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }

}

trait AuthWithWeakCreds extends Setup with AuthorityTest {
  val routeToIv=false
  val routeToTwoFactor=true

  override lazy val authConnector = new TestAuthConnector(None) {
    lazy val exception = new AccountWithWeakCredStrength("Forbidden to access since weak cred strength")
    override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future.successful(Accounts(Some(nino), None, routeToIv, routeToTwoFactor, "102030394AAA"))
    override def grantAccess(taxId:Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
  }

  override lazy val testAccess = new TestAccessCheck(authConnector)
  override lazy val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new CustomerProfileController {
    val app = "AuthWithWeakCreds Customer Profile"
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
  override lazy val entityConnector = new TestEntityResolverConnector(PreferencesCreated, defaultPreference)

  override lazy val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, prefererencesConnector)

  override val controller = new CustomerProfileController {
    val app = "SandboxPaperlessCreated Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait SandboxPaperlessFailed extends SandboxPaperlessCreated {
  override lazy val entityConnector = new TestEntityResolverConnector(PreferencesFailure, defaultPreference)
  override lazy val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnectorTest, prefererencesConnector)

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
    override val upgradeRequiredCheckerService = upgradeService(upgrade)
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait SandboxNativeVersionChecker extends Setup {
  lazy val upgrade = false

  val controller = new NativeVersionCheckerController {
    val app = "Test-Native-Version-Checker"
    override val upgradeRequiredCheckerService = SandboxUpgradeRequiredCheckerService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

class LoggerLikeStub extends LoggerLike {
  import scala.collection.mutable.Buffer

  val logMessages: Buffer[String] = Buffer()

  override val logger: Logger = null

  override def warn(msg: => String) = logMessages += msg
}