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

package uk.gov.hmrc.customerprofile.services

import uk.gov.hmrc.api.service.Auditor
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.customerprofile.config.MicroserviceAuditConnector
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


trait CustomerProfileService {

  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts]

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails]

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus]

  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]]
}


trait LiveCustomerProfileService extends CustomerProfileService with Auditor {

  def authConnector: AuthConnector

  def citizenDetailsConnector: CitizenDetailsConnector

  def entityResolver: EntityResolverConnector

  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts] =
    withAudit("getAccounts", Map.empty) {
      authConnector.accounts()
    }

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails] =
    withAudit("getPersonalDetails", Map("nino" -> nino.value)) {
      citizenDetailsConnector.personDetails(nino)
    }

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] =
    withAudit("getPaperlessSettings", Map("accepted" -> settings.generic.accepted.toString)) {
      entityResolver.paperlessSettings(settings)
    }

  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] =
    withAudit("getAccounts", Map.empty) {
      entityResolver.getPreferences()
    }

}

object SandboxCustomerProfileService extends CustomerProfileService with FileResource {

  private val nino = Nino("CS700100A")

  private val personDetailsSandbox = PersonDetails("etag", Person(Some("Firstname"), Some("Middlename"), Some("Lastname"),
    Some("LM"), Some("Mr"), None, Some("Male"), None, None), None, None)

  private val accounts = Accounts(Some(nino), None)

  def getAccounts()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Accounts] = {
    Future.successful(accounts)
  }

  def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonDetails] = {
    Future(personDetailsSandbox)
  }

  def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PreferencesStatus] = {
    Future(PreferencesExists)
  }

  def getPreferences()(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Preference]] = ???
}

object LiveCustomerProfileService extends LiveCustomerProfileService {
  val authConnector: AuthConnector = AuthConnector

  val citizenDetailsConnector: CitizenDetailsConnector = CitizenDetailsConnector

  val entityResolver: EntityResolverConnector = EntityResolverConnector

  val auditConnector: AuditConnector = MicroserviceAuditConnector
}
