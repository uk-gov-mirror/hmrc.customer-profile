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
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControl, AccountAccessControlForSandbox, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, LiveCustomerProfileService, SandboxCustomerProfileService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook

import scala.concurrent.{ExecutionContext, Future}

class TestCitizenDetailsConnector(httpResponse:Future[HttpResponse]) extends CitizenDetailsConnector {
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
  override def paperlessSettings(paperless: Paperless)(implicit hc: HeaderCarrier, ex : ExecutionContext): Future[PreferencesStatus] =  Future.successful(preferencesStatus)
}

// TODO...basic auditing!
class TestAuthConnector(nino:Option[Nino]) extends AuthConnector {
  override val serviceUrl: String = "someUrl"

  override def serviceConfidenceLevel: ConfidenceLevel = ???

  override def http: HttpGet = ???

  override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future(Accounts(nino, None))
  override def hasNino()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future(Unit)
}

class TestCustomerProfileService(testCDConnector:CitizenDetailsConnector, testAuthConnector:TestAuthConnector, testEntityResolver:EntityResolverConnector) extends LiveCustomerProfileService {

  override val citizenDetailsConnector = testCDConnector
  override val authConnector = testAuthConnector
  override def entityResolver = testEntityResolver
}

class TestAccessCheck(testAuthConnector:TestAuthConnector) extends AccountAccessControl {
  override val authConnector: AuthConnector = testAuthConnector
}

class TestAccountAccessControlWithAccept(testAccessCheck:AccountAccessControl) extends AccountAccessControlWithHeaderCheck {
  override val accessControl: AccountAccessControl = testAccessCheck
}

trait Setup {
  implicit val hc = HeaderCarrier()

  val emptyRequest = FakeRequest()
  val emptyRequestWithHeader = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

  val paperlessJsonBody = Json.toJson(Paperless(TermsAccepted(true), EmailAddress("a@b.com")))
  val paperlessRequest = FakeRequest().withJsonBody(paperlessJsonBody)
    .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")

  val nino = Nino("CS700100A")
  val testAccount = Accounts(Some(nino), None)
  val person = PersonDetails("etag", Person(Some("Firstname"), Some("Middlename"), Some("Lastname"),
    Some("LM"), Some("Mr"), None, Some("Male"), None, None), None, None)
  val customerProfile = CustomerProfile(testAccount, person)

  lazy val http200ResponseCid = Future.successful(HttpResponse(200, Some(Json.toJson(person))))

  val authConnector = new TestAuthConnector(Some(nino))
  val cdConnector = new TestCitizenDetailsConnector(http200ResponseCid)
  val entityConnector = new TestEntityResolverConnector(PreferencesExists)

  val testAccess = new TestAccessCheck(authConnector)

  val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
  val testCustomerProfileService = new TestCustomerProfileService(cdConnector, authConnector, entityConnector)

  val testSandboxPersonalIncomeService = SandboxCustomerProfileService
  val sandboxCompositeAction = AccountAccessControlForSandbox
}

trait Success extends Setup {
  val controller = new CustomerProfileController {
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait AuthWithoutNino extends Setup {

  override val authConnector =  new TestAuthConnector(None) {
    override def hasNino()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = Future.failed(new uk.gov.hmrc.play.http.Upstream4xxResponse("Error", 401, 401))
  }

  override val testAccess = new TestAccessCheck(authConnector)
  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new CustomerProfileController {
    override val service: CustomerProfileService = testCustomerProfileService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
  }
}

trait SandboxSuccess extends Setup {
  val controller = new CustomerProfileController {
    override val service: CustomerProfileService = testSandboxPersonalIncomeService
    override val accessControl: AccountAccessControlWithHeaderCheck = sandboxCompositeAction
  }
}
