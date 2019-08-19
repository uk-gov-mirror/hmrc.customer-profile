/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{parse, toJson}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.SessionRecordNotFound
import uk.gov.hmrc.customerprofile.auth.{AccountAccessControl, NinoNotFoundOnAccount}
import uk.gov.hmrc.customerprofile.connector.{PreferencesDoesNotExist, PreferencesFailure, _}
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.Verified
import uk.gov.hmrc.customerprofile.domain.{Paperless, _}
import uk.gov.hmrc.customerprofile.services.CustomerProfileService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class LiveCustomerProfileControllerSpec extends WordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with MockFactory {
  val service:       CustomerProfileService = mock[CustomerProfileService]
  val accessControl: AccountAccessControl   = mock[AccountAccessControl]

  val controller: LiveCustomerProfileController =
    new LiveCustomerProfileController(service, accessControl, citizenDetailsEnabled = true, stubControllerComponents())

  val nino = Nino("CS700100A")
  val journeyId: String = randomUUID().toString
  val emptyRequest = FakeRequest()
  val acceptheader:               String                              = "application/vnd.hmrc.1.0+json"
  val requestWithAcceptHeader:    FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("Accept" -> acceptheader)
  val requestWithoutAcceptHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("Authorization" -> "Some Header")

  val invalidPostRequest: FakeRequest[JsValue] =
    FakeRequest().withBody(parse("""{ "blah" : "blah" }""")).withHeaders(HeaderNames.ACCEPT → acceptheader)

  def authSuccess(maybeNino: Option[Nino] = None) =
    (accessControl.grantAccess(_: Option[Nino])(_: HeaderCarrier, _: ExecutionContext)).expects(maybeNino, *, *).returns(Future.successful(()))

  def authError(e: Exception, maybeNino: Option[Nino] = None) =
    (accessControl.grantAccess(_: Option[Nino])(_: HeaderCarrier, _: ExecutionContext)).expects(maybeNino, *, *).returns(Future failed e)

  "getAccounts" should {
    def mockGetAccounts(result: Future[Accounts]) =
      (service.getAccounts()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(result)

    "return account details without journey id" in {
      val accounts: Accounts = Accounts(Some(nino), None, routeToIV = false, routeToTwoFactor = false, "102030394AAA")
      mockGetAccounts(Future successful accounts)

      val result = controller.getAccounts(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(accounts)
    }

    "return account details with journey id" in {
      val accounts: Accounts = Accounts(Some(nino), None, routeToIV = false, routeToTwoFactor = false, "102030394AAA")
      mockGetAccounts(Future successful accounts)

      val result = controller.getAccounts(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(accounts)
    }

    "propagate 401" in {
      mockGetAccounts(Future failed new SessionRecordNotFound)

      val result = controller.getAccounts(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      mockGetAccounts(Future failed new NinoNotFoundOnAccount("no nino"))

      val result = controller.getAccounts(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return status code 406 when the headers are invalid" in {
      val result = controller.getAccounts(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      mockGetAccounts(Future failed new RuntimeException())

      val result = controller.getAccounts(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
  }

  "getPersonalDetails" should {
    def mockGetAccounts(result: Future[PersonDetails]) =
      (service.getPersonalDetails(_: Nino)(_: HeaderCarrier, _: ExecutionContext)).expects(nino, *, *).returns(result)

    "return personal details without journey id" in {
      val person = PersonDetails(
        "etag",
        Person(Some("Firstname"), Some("Lastname"), Some("Middle"), Some("Intial"), Some("Title"), Some("Honours"), Some("sex"), None, None),
        None)

      authSuccess(Some(nino))
      mockGetAccounts(Future successful person)

      val result = controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(person)
    }

    "return personal details with journey id" in {
      val person = PersonDetails(
        "etag",
        Person(Some("Firstname"), Some("Lastname"), Some("Middle"), Some("Intial"), Some("Title"), Some("Honours"), Some("sex"), None, None),
        None)

      authSuccess(Some(nino))
      mockGetAccounts(Future successful person)

      val result = controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(person)
    }

    "propagate 401" in {
      authError(new SessionRecordNotFound, Some(nino))

      val result = controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      authError(new NinoNotFoundOnAccount("no nino"), Some(nino))

      val result = controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return status code 406 when the headers are invalid" in {
      val result = controller.getPersonalDetails(nino, journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      authSuccess(Some(nino))
      mockGetAccounts(Future failed new RuntimeException())

      val result = controller.getPersonalDetails(nino, journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
  }

  "getPreferences" should {
    def mockGetPreferences(result: Future[Option[Preference]]) =
      (service.getPreferences()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(result)

    "return preferences without journeyId" in {
      val preference: Preference = Preference(digital = true, Some(EmailPreference(EmailAddress("old@old.com"), Verified)))

      authSuccess()
      mockGetPreferences(Future successful Some(preference))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(preference)
    }

    "return preferences with journeyId" in {
      val preference: Preference = Preference(digital = true, Some(EmailPreference(EmailAddress("old@old.com"), Verified)))

      authSuccess()
      mockGetPreferences(Future successful Some(preference))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe toJson(preference)
    }

    "handle no preferences found" in {
      authSuccess()
      mockGetPreferences(Future successful None)

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 404
    }

    "propagate 401" in {
      authError(new SessionRecordNotFound)

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      authError(new NinoNotFoundOnAccount("no nino"))

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return status code 406 when the headers are invalid" in {
      val result = controller.getPreferences(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      authSuccess()
      mockGetPreferences(Future failed new RuntimeException())

      val result = controller.getPreferences(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
  }

  "paperlessSettingsOptIn" should {
    def mockPaperlessSettings(settings: Paperless, result: Future[PreferencesStatus]) =
      (service.paperlessSettings(_: Paperless)(_: HeaderCarrier, _: ExecutionContext)).expects(settings, *, *).returns(result)

    val newEmail          = EmailAddress("new@new.com")
    val paperlessSettings = Paperless(TermsAccepted(true), newEmail)

    val validPaperlessSettingsRequest: FakeRequest[JsValue] =
      FakeRequest().withBody(toJson(paperlessSettings)).withHeaders(HeaderNames.ACCEPT → acceptheader)
    val paperlessSettingsRequestWithoutAcceptHeader: FakeRequest[JsValue] = FakeRequest().withBody(toJson(paperlessSettings))

    "opt in for a user with no preferences without journey id" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future successful PreferencesCreated)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 201
    }

    "opt in for a user with no preferences with journey id" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future successful PreferencesCreated)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 201
    }

    "opt in for a user with existing preferences" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future successful PreferencesExists)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 204
    }

    "return 404 where preferences do not exist" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future successful NoPreferenceExists)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 404
    }

    "return 409 for request without email" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future successful EmailNotExist)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 409
    }

    "propagate errors from the service" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future successful PreferencesFailure)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)

      status(result) shouldBe 500
    }

    "propagate 401 for auth failure" in {
      authError(new SessionRecordNotFound)

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      authError(new NinoNotFoundOnAccount("no nino"))

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)
      status(result) shouldBe 403
    }

    "return status code 406 when no accept header is provided" in {
      val result = controller.paperlessSettingsOptIn(journeyId)(paperlessSettingsRequestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 400 for an invalid form" in {
      authSuccess()
      val result = controller.paperlessSettingsOptIn(journeyId)(invalidPostRequest)
      status(result) shouldBe 400
    }

    "return 500 for an unexpected error" in {
      authSuccess()
      mockPaperlessSettings(paperlessSettings, Future failed new RuntimeException())

      val result = controller.paperlessSettingsOptIn(journeyId)(validPaperlessSettingsRequest)
      status(result) shouldBe 500
    }
  }

  "paperlessSettingsOptOut" should {
    def mockPaperlessSettingsOptOut(result: Future[PreferencesStatus]) =
      (service.paperlessSettingsOptOut()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(result)

    "opt out for existing preferences without journey id" in {
      authSuccess()
      mockPaperlessSettingsOptOut(Future successful PreferencesExists)

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 204
    }

    "opt out for existing preferences with journey id" in {
      authSuccess()
      mockPaperlessSettingsOptOut(Future successful PreferencesExists)

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 204
    }

    "opt out without existing preferences and journey id" in {
      authSuccess()
      mockPaperlessSettingsOptOut(Future successful PreferencesCreated)

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 201
    }

    "return 404 where preference does not exist" in {
      authSuccess()
      mockPaperlessSettingsOptOut(Future successful PreferencesDoesNotExist)

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 404
    }

    "return 500 on service error" in {
      authSuccess()
      mockPaperlessSettingsOptOut(Future successful PreferencesFailure)

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)

      status(result) shouldBe 500
    }

    "propagate 401 for auth failure" in {
      authError(new SessionRecordNotFound)

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      authError(new NinoNotFoundOnAccount("no nino"))

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 403
    }

    "return status code 406 when no accept header is provided" in {
      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 500 for an unexpected error" in {
      authSuccess()
      mockPaperlessSettingsOptOut(Future failed new RuntimeException())

      val result = controller.paperlessSettingsOptOut(journeyId)(requestWithAcceptHeader)
      status(result) shouldBe 500
    }
  }

  "preferencesPendingEmail" should {

    def mockPendingEmail(changeEmail: ChangeEmail, result: Future[PreferencesStatus]) =
      (service.setPreferencesPendingEmail(_: ChangeEmail)(_: HeaderCarrier, _: ExecutionContext)).expects(changeEmail, *, *).returns(result)

    val newEmail          = EmailAddress("new@new.com")
    val changeEmail = ChangeEmail(newEmail)

    val validPendingEmailRequest: FakeRequest[JsValue] =
      FakeRequest().withBody(toJson(changeEmail)).withHeaders(HeaderNames.ACCEPT → acceptheader)
    val changeEmailRequestWithoutAcceptHeader: FakeRequest[JsValue] = FakeRequest().withBody(toJson(changeEmail))

    "successful pending email change" in {
      authSuccess()
      mockPendingEmail(changeEmail, Future successful EmailUpdateOk)

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 204
    }

    "return 404 where preferences do not exist" in {
      authSuccess()
      mockPendingEmail(changeEmail, Future successful NoPreferenceExists)

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 404
    }

    "return 409 for request without email" in {
      authSuccess()
      mockPendingEmail(changeEmail, Future successful EmailNotExist)

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 409
    }

    "propagate errors from the service" in {
      authSuccess()
      mockPendingEmail(changeEmail, Future successful PreferencesFailure)

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)

      status(result) shouldBe 500
    }

    "propagate 401 for auth failure" in {
      authError(new SessionRecordNotFound)

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
      status(result) shouldBe 401
    }

    "return 403 if the user has no nino" in {
      authError(new NinoNotFoundOnAccount("no nino"))

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
      status(result) shouldBe 403
    }

    "return status code 406 when no accept header is provided" in {
      val result = controller.preferencesPendingEmail(journeyId)(changeEmailRequestWithoutAcceptHeader)
      status(result) shouldBe 406
    }

    "return 400 for an invalid form" in {
      authSuccess()
      val result = controller.preferencesPendingEmail(journeyId)(invalidPostRequest)
      status(result) shouldBe 400
    }

    "return 500 for an unexpected error" in {
      authSuccess()
      mockPendingEmail(changeEmail, Future failed new RuntimeException())

      val result = controller.preferencesPendingEmail(journeyId)(validPendingEmailRequest)
      status(result) shouldBe 500
    }
  }

}
