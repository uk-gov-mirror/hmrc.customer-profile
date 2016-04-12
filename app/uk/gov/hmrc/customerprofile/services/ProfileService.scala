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

import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.customerprofile.config.MicroserviceAuditConnector
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait CustomerProfileService {
  def getProfile()(implicit hc:HeaderCarrier): Future[CustomerProfile]

  def getAccounts()(implicit hc:HeaderCarrier): Future[Accounts]

  def getPersonalDetails(nino:Nino)(implicit hc:HeaderCarrier): Future[PersonDetails]

  def paperlessSettings(settings:Paperless)(implicit hc:HeaderCarrier): Future[PreferencesStatus]
}

trait LiveCustomerProfileService extends CustomerProfileService {
  def authConnector: AuthConnector

  def citizenDetailsConnector: CitizenDetailsConnector

  def entityResolver : EntityResolverConnector

  def audit(service:String, details:Map[String, String])(implicit hc:HeaderCarrier) = {
    def auditResponse(): Unit = {
      MicroserviceAuditConnector.sendEvent(
        DataEvent("customer-profile", "ServiceResponseSent",
          tags = Map("transactionName" -> service),
          detail = details))
    }
  }

  def withAudit[T](service: String, details: Map[String, String])(func:Future[T])(implicit hc:HeaderCarrier) = {
    audit(service, details) // No need to wait!
    func
  }

  def getProfile()(implicit hc: HeaderCarrier): Future[CustomerProfile] = {
    withAudit("getProfile", Map.empty) {
      CustomerProfile.create(authConnector.accounts, (nino) => citizenDetailsConnector.personDetails(nino.get))
    }
  }

  def getAccounts()(implicit hc: HeaderCarrier): Future[Accounts] = {
    withAudit("getAccounts", Map.empty) {
      authConnector.accounts()
    }
  }

  override def getPersonalDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetails] = {
    withAudit("getPersonalDetails", Map("nino" -> nino.value)) {
      citizenDetailsConnector.personDetails(nino)
    }
  }

  override def paperlessSettings(settings: Paperless)(implicit hc: HeaderCarrier): Future[PreferencesStatus] = {
    withAudit("getPaperlessSettings", Map("accepted" -> settings.generic.accepted.toString)) {
      entityResolver.paperlessSettings(settings)
    }
  }
}

object SandboxCustomerProfileService extends CustomerProfileService with FileResource {
  val nino=Nino("CS700100A")
  val personDetailsSandbox = PersonDetails("etag", Person(Some("Firstname"), Some("Middlename"), Some("Lastname"),
    Some("LM"), Some("Mr"), None, Some("Male"), None, None), None, None)
  val accounts = Accounts(Some(nino), None)

  def getProfile()(implicit hc: HeaderCarrier): Future[CustomerProfile] = {
    Future.successful(CustomerProfile(accounts, personDetailsSandbox))
  }

  def getAccounts()(implicit hc: HeaderCarrier):  Future[Accounts] = {
    Future.successful(accounts)
  }

  def getPersonalDetails(nino:Nino)(implicit hc: HeaderCarrier): Future[PersonDetails] = {
    Future(personDetailsSandbox)
  }

  def paperlessSettings(settings:Paperless)(implicit hc: HeaderCarrier): Future[PreferencesStatus] = {
    Future(PreferencesExists)
  }
}

object LiveCustomerProfileService extends LiveCustomerProfileService {
  override val authConnector: AuthConnector = AuthConnector

  override val citizenDetailsConnector: CitizenDetailsConnector = CitizenDetailsConnector

  override def entityResolver: EntityResolverConnector = EntityResolverConnector
}
