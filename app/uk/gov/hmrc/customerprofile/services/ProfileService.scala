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

package uk.gov.hmrc.customerprofile.services

import java.util.UUID

import org.joda.time.DateTime.parse
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.api.service.Auditor
import uk.gov.hmrc.customerprofile.config.MicroserviceAuditConnector
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControl, NinoNotFoundOnAccount}
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

trait CustomerProfileService {

  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts]

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails]

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus]

  def paperlessSettingsOptOut()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus]

  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]]

  def setPreferencesPendingEmail(changeEmail: ChangeEmail)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[EmailUpdateStatus]
}


trait LiveCustomerProfileService extends CustomerProfileService with Auditor {
  val accountAccessControl: AccountAccessControl

  def citizenDetailsConnector: CitizenDetailsConnector

  def entityResolver: EntityResolverConnector

  def preferencesConnector: PreferencesConnector

  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts] =
    withAudit("getAccounts", Map.empty) {
      accountAccessControl.accounts
    }

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails] =
    withAudit("getPersonalDetails", Map("nino" -> nino.value)) {
      citizenDetailsConnector.personDetails(nino)
    }

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    withAudit("paperlessSettings", Map("accepted" -> settings.generic.accepted.toString)) {
      entityResolver.paperlessSettings(settings)
    }

  def paperlessSettingsOptOut()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    withAudit("paperlessSettingsOptOut", Map.empty) {
      entityResolver.paperlessOptOut()
    }

  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] =
    withAudit("getAccounts", Map.empty) {
      entityResolver.getPreferences()
    }

  def setPreferencesPendingEmail(changeEmail: ChangeEmail)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[EmailUpdateStatus] =
    withAudit("updatePendingEmailPreference", Map("email" → changeEmail.email)) {
      for{
        account   ← getAccounts()
        entity    ← entityResolver.getEntityIdByNino(account.nino.getOrElse(throw new NinoNotFoundOnAccount("")))
        response  ← preferencesConnector.updatePendingEmail(changeEmail, entity._id)
      } yield response
    }

}

object SandboxCustomerProfileService extends CustomerProfileService with FileResource {

  private val nino = Nino("CS700100A")

  private val personDetailsSandbox =
    PersonDetails(
      "etag",
      Person(Some("Jennifer"), None, Some("Thorsteinson"), None, Some("Ms"), None, Some("Female"), Some(parse("1999-01-31")), Some(nino)),
      Some(Address(Some("999 Big Street"), Some("Worthing"), Some("West Sussex"), None, None, Some("BN99 8IG"), None, None, None )))

  private val accounts = Accounts(Some(nino), None, false, false, UUID.randomUUID().toString)

  private val email = EmailAddress("name@email.co.uk")

  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts] = {
    Future.successful(accounts)
  }

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails] = {
    Future(personDetailsSandbox)
  }

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    Future(PreferencesExists)

  override def paperlessSettingsOptOut()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    Future(PreferencesExists)


  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] = {
    Future(Some(Preference(true, Some(EmailPreference(email, Status.Verified)))))
  }

  def setPreferencesPendingEmail(changeEmail: ChangeEmail)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[EmailUpdateStatus] = {
    Future(EmailUpdateOk)
  }
}

object LiveCustomerProfileService extends LiveCustomerProfileService {
  val citizenDetailsConnector: CitizenDetailsConnector = CitizenDetailsConnector

  val entityResolver: EntityResolverConnector = EntityResolverConnector

  val auditConnector: AuditConnector = MicroserviceAuditConnector

  val preferencesConnector: PreferencesConnector = PreferencesConnector

  override val accountAccessControl: AccountAccessControl = AccountAccessControl
}
