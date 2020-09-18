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

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import play.api.Configuration
import uk.gov.hmrc.customerprofile.auth.{AccountAccessControl, NinoNotFoundOnAccount}
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.service.Auditor

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomerProfileService @Inject() (
  citizenDetailsConnector:       CitizenDetailsConnector,
  preferencesConnector:          PreferencesConnector,
  entityResolver:                EntityResolverConnector,
  val accountAccessControl:      AccountAccessControl,
  val appNameConfiguration:      Configuration,
  val auditConnector:            AuditConnector,
  @Named("appName") val appName: String)
    extends Auditor {

  def getAccounts(
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Accounts] =
    withAudit("getAccounts", Map.empty) {
      accountAccessControl.accounts(journeyId)
    }

  def getPersonalDetails(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[PersonDetails] =
    withAudit("getPersonalDetails", Map("nino" -> nino.value)) {
      citizenDetailsConnector.personDetails(nino)
    }

  def paperlessSettings(
    settings:    Paperless,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[PreferencesStatus] =
    withAudit("paperlessSettings", Map("accepted" -> settings.generic.accepted.toString)) {
      for {
        preferences ← entityResolver.getPreferences()
        status ← preferences.fold(
                  entityResolver
                    .paperlessSettings(settings.copy(generic = settings.generic.copy(accepted = Some(true))))
                ) { preference =>
                  if (preference.digital) setPreferencesPendingEmail(ChangeEmail(settings.email.value), journeyId)
                  else
                    entityResolver.paperlessSettings(
                      settings.copy(generic = settings.generic.copy(accepted = Some(true)))
                    )
                }
      } yield status
    }

  def paperlessSettingsOptOut(
    paperlessOptOut: PaperlessOptOut
  )(implicit hc:     HeaderCarrier,
    ex:              ExecutionContext
  ): Future[PreferencesStatus] =
    withAudit("paperlessSettingsOptOut", Map.empty) {
      entityResolver.paperlessOptOut(
        paperlessOptOut.copy(generic = paperlessOptOut.generic.copy(accepted = Some(false)))
      )
    }

  def getPreferences(
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Option[Preference]] =
    withAudit("getPreferences", Map.empty) {
      entityResolver.getPreferences()
    }

  def setPreferencesPendingEmail(
    changeEmail: ChangeEmail,
    journeyId:   JourneyId
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[PreferencesStatus] =
    withAudit("updatePendingEmailPreference", Map("email" → changeEmail.email)) {
      for {
        account ← getAccounts(journeyId)
        entity ← entityResolver.getEntityIdByNino(account.nino.getOrElse(throw new NinoNotFoundOnAccount("")))
        response ← preferencesConnector.updatePendingEmail(changeEmail, entity._id)
      } yield response
    }

}
