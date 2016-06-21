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

package uk.gov.hmrc.customerprofile.controllers

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.customerprofile.config.{MicroserviceAuditConnector, ServicesCircuitBreaker}
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControl, AccountAccessControlCheckOff, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, LiveCustomerProfileService, SandboxCustomerProfileService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import play.api.test.Helpers._


class TestCitizenDetailsConnector(httpResponse: Future[HttpResponse]) extends CitizenDetailsConnector {
  override lazy val citizenDetailsConnectorUrl = "someUrl"
  override lazy val http: HttpGet = new HttpGet {
    override val hooks: Seq[HttpHook] = NoneRequired

    override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = httpResponse
  }
}

class TestEntityResolverConnector(preferencesStatus: PreferencesStatus) extends EntityResolverConnector with ServicesConfig with ServicesCircuitBreaker with play.api.http.Status {
  this: ServicesCircuitBreaker =>
  override def http: HttpGet with HttpPost with HttpPut = ???

  override def serviceUrl: String = ???

  override def paperlessSettings(paperless: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] = {
    Future.successful(preferencesStatus)
  }
}

class TestAuthConnector(nino: Option[Nino]) extends AuthConnector {
  override val serviceUrl: String = "someUrl"

  override def serviceConfidenceLevel: ConfidenceLevel = ???

  override def http: HttpGet = ???

  override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future(Accounts(nino, None, false, false, "102030394AAA"))

  override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future(Unit)
}

class TestCustomerProfileService(testCDConnector: CitizenDetailsConnector,
                                 testAuthConnector: TestAuthConnector,
                                 testEntityResolver: EntityResolverConnector,
                                 testAuditConnector: AuditConnector) extends LiveCustomerProfileService {

  val citizenDetailsConnector = testCDConnector
  val authConnector = testAuthConnector
  val entityResolver = testEntityResolver
  val auditConnector: AuditConnector = testAuditConnector
}

class TestAccessCheck(testAuthConnector: TestAuthConnector) extends AccountAccessControl {
  override val authConnector: AuthConnector = testAuthConnector
}

class TestAccountAccessControlWithAccept(testAccessCheck: AccountAccessControl) extends AccountAccessControlWithHeaderCheck {
  override val accessControl: AccountAccessControl = testAccessCheck
}

trait Setup {
  implicit val hc = HeaderCarrier()

  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val paperlessJsonBody = Json.toJson(Paperless(TermsAccepted(true), EmailAddress("a@b.com")))
  val noNinoOnAccount = Json.parse("""{"code":"UNAUTHORIZED","message":"NINO does not exist on account"}""")
  val lowCL = Json.parse("""{"code":"LOW_CONFIDENCE_LEVEL","message":"Confidence Level on account does not allow access"}""")
  val weakCredStrength = Json.parse("""{"code":"WEAK_CRED_STRENGTH","message":"Credential Strength on account does not allow access"}""")
  val paperlessRequestNoAccept = FakeRequest().withBody(paperlessJsonBody).withHeaders("Content-Type" -> "application/json")
  val paperlessRequest = FakeRequest().withBody(paperlessJsonBody)
    .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")

  val nino = Nino("CS700100A")
  val testAccount = Accounts(Some(nino), None, false, false,"102030394AAA")
  val person = PersonDetails("etag", Person(Some("Nuala"), Some("Theo"), Some("O'Shea"),
    Some("LM"), Some("Mr"), None, Some("Male"), None, None), None, None)

  lazy val http200ResponseCid = Future.successful(HttpResponse(200, Some(Json.toJson(person))))

  lazy val authConnector = new TestAuthConnector(Some(nino))
  lazy val cdConnector = new TestCitizenDetailsConnector(http200ResponseCid)
  lazy val entityConnector = new TestEntityResolverConnector(PreferencesExists)

  lazy val testAccess = new TestAccessCheck(authConnector)

  lazy val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
  lazy val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnector)

  lazy val testSandboxCustomerProfileService = SandboxCustomerProfileService
  lazy val sandboxCompositeAction = AccountAccessControlCheckOff
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

trait AuthWithoutNino extends Setup with AuthorityTest {

  override lazy val authConnector = new TestAuthConnector(None) {
    lazy val exception = new NinoNotFoundOnAccount("The user must have a National Insurance Number")
    override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future.failed(exception)
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
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
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
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
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(exception)
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
  override lazy val entityConnector = new TestEntityResolverConnector(PreferencesCreated)

  override lazy val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnector)

  override val controller = new CustomerProfileController {
    val app = "SandboxPaperlessCreated Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait SandboxPaperlessFailed extends SandboxPaperlessCreated {
  override lazy val entityConnector = new TestEntityResolverConnector(PreferencesFailure)
  override lazy val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector, MicroserviceAuditConnector)

  override val controller = new CustomerProfileController {
    val app = "SandboxPaperlessFailed Customer Profile"
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}