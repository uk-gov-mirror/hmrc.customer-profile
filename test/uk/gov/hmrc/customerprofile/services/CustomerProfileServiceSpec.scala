/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.services

import eu.timepit.refined.auto._
import org.scalamock.handlers.CallHandler3
import org.scalamock.matchers.MatcherBase
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customerprofile.auth.AccountAccessControl
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.Verified
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CustomerProfileServiceSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with MockFactory {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val appNameConfiguration: Configuration = mock[Configuration]
  val auditConnector: AuditConnector = mock[AuditConnector]
  val journeyId: JourneyId = "b6ef25bc-8f5e-49c8-98c5-f039f39e4557"
  val appName: String = "customer-profile"

  def mockAudit(
    transactionName: String,
    detail: Map[String, String] = Map.empty
  ): CallHandler3[DataEvent, HeaderCarrier, ExecutionContext, Future[
    AuditResult
  ]] = {
    def dataEventWith(auditSource: String,
                      auditType: String,
                      tags: Map[String, String]): MatcherBase =
      argThat { (dataEvent: DataEvent) =>
        dataEvent.auditSource.equals(auditSource) &&
        dataEvent.auditType.equals(auditType) &&
        dataEvent.tags.equals(tags) &&
        dataEvent.detail.equals(detail)
      }

    (appNameConfiguration
      .getString(_: String, _: Option[Set[String]]))
      .expects("appName", None)
      .returns(Some(appName))
      .anyNumberOfTimes()

    (auditConnector
      .sendEvent(_: DataEvent)(_: HeaderCarrier, _: ExecutionContext))
      .expects(
        dataEventWith(
          appName,
          auditType = "ServiceResponseSent",
          tags = Map("transactionName" -> transactionName)
        ),
        *,
        *
      )
      .returns(Future successful Success)
  }

  val citizenDetailsConnector: CitizenDetailsConnector =
    mock[CitizenDetailsConnector]
  val preferencesConnector: PreferencesConnector = mock[PreferencesConnector]
  val entityResolver: EntityResolverConnector = mock[EntityResolverConnector]
  val accountAccessControl: AccountAccessControl = mock[AccountAccessControl]

  val service =
    new CustomerProfileService(
      citizenDetailsConnector,
      preferencesConnector,
      entityResolver,
      accountAccessControl,
      appNameConfiguration,
      auditConnector,
      "customer-profile"
    )

  val existingDigitalPreference: Preference = existingPreferences(
    digital = true
  )
  val existingNonDigitalPreference: Preference = existingPreferences(
    digital = false
  )

  val newEmail = EmailAddress("new@new.com")
  val newPaperlessSettings = Paperless(TermsAccepted(true), newEmail, Some("en"))

  val nino = Nino("CS700100A")
  val accounts: Accounts = Accounts(
    Some(nino),
    None,
    routeToIV = false,
    routeToTwoFactor = false,
    "journeyId"
  )

  def existingPreferences(digital: Boolean): Preference =
    Preference(
      digital,
      Some(EmailPreference(EmailAddress("old@old.com"), Verified))
    )

  def mockGetAccounts() = {
    mockAudit(transactionName = "getAccounts")
    (accountAccessControl
      .accounts(_: JourneyId)(_: HeaderCarrier))
      .expects(*, *)
      .returns(Future successful accounts)
  }

  def mockGetPreferences(maybeExistingPreferences: Option[Preference]) =
    (entityResolver
      .getPreferences()(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *)
      .returns(Future successful maybeExistingPreferences)

  def mockAuditPaperlessSettings() =
    mockAudit(
      transactionName = "paperlessSettings",
      detail = Map("accepted" -> newPaperlessSettings.generic.accepted.toString)
    )

  def mockPaperlessSettings(status: PreferencesStatus) =
    (entityResolver
      .paperlessSettings(_: Paperless)(_: HeaderCarrier, _: ExecutionContext))
      .expects(newPaperlessSettings, *, *)
      .returns(Future successful status)

  def mockGetEntityIdAndUpdatePendingEmailWithAudit() = {
    val entity: Entity = Entity("entityId")

    mockAudit(
      transactionName = "updatePendingEmailPreference",
      detail = Map("email" -> newEmail.value)
    )
    (entityResolver
      .getEntityIdByNino(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returns(Future successful entity)

    (preferencesConnector
      .updatePendingEmail(_: ChangeEmail, _: String)(
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(ChangeEmail(newEmail.value), entity._id, *, *)
      .returns(Future.successful(EmailUpdateOk))
  }

  "getAccounts" should {
    "audit and return accounts" in {
      mockGetAccounts()
      await(service.getAccounts(journeyId)) shouldBe accounts
    }
  }

  "getPersonalDetails" should {
    "audit and return accounts" in {
      val person = PersonDetails(
        Person(
          Some("Firstname"),
          Some("Lastname"),
          Some("Middle"),
          Some("Intial"),
          Some("Title"),
          Some("Honours"),
          Some("sex"),
          None,
          None
        ),
        None
      )

      mockAudit(
        transactionName = "getPersonalDetails",
        detail = Map("nino" -> nino.value)
      )
      (citizenDetailsConnector
        .personDetails(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
        .expects(nino, *, *)
        .returns(Future successful person)
      val personalDetails = await(service.getPersonalDetails(nino))

      personalDetails shouldBe person
      personalDetails.person.shortName shouldBe Some("Firstname Middle")
      personalDetails.person.fullName shouldBe "Title Firstname Lastname Middle Honours"
    }
  }

  "getPreferences" should {
    "audit and return preferences" in {
      mockAudit(transactionName = "getPreferences")
      mockGetPreferences(Some(existingDigitalPreference))

      await(service.getPreferences()) shouldBe Some(existingDigitalPreference)
    }
  }

  "paperlessSettings" should {
    "update the email for a user who already has a defined digital preference" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(Some(existingDigitalPreference))
      mockGetAccounts()
      mockGetEntityIdAndUpdatePendingEmailWithAudit()

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe EmailUpdateOk
    }

    "set the digital preference to true and update the email for a user who already has a defined non-digital preference" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(Some(existingNonDigitalPreference))
      mockPaperlessSettings(PreferencesExists)

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe PreferencesExists
    }

    "set the digital preference to true and set the email for a user who has no defined preference" in {
      mockAuditPaperlessSettings()
      mockGetPreferences(None)
      mockPaperlessSettings(PreferencesCreated)

      await(service.paperlessSettings(newPaperlessSettings, journeyId)) shouldBe PreferencesCreated
    }
  }

  "paperlessSettingsOptOut" should {
    "audit and opt the user out" in {
      mockAudit(transactionName = "paperlessSettingsOptOut")
      (entityResolver
        .paperlessOptOut(_: PaperlessOptOut)(_: HeaderCarrier, _: ExecutionContext))
        .expects(PaperlessOptOut(TermsAccepted(false), Some("en")), *, *)
        .returns(Future successful PreferencesExists)

      await(service.paperlessSettingsOptOut(PaperlessOptOut(TermsAccepted(false), Some("en")))) shouldBe PreferencesExists
    }
  }

}
