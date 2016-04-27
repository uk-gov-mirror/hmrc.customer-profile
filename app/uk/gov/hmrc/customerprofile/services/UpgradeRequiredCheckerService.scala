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
import uk.gov.hmrc.customerprofile.config.MicroserviceAuditConnector
import uk.gov.hmrc.customerprofile.domain.{DeviceVersion, ValidateAppVersion}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait UpgradeRequiredCheckerService {
  def upgradeRequired(deviceVersion: DeviceVersion)(implicit hc: HeaderCarrier, ex: ExecutionContext) : Future[Boolean]
}

trait LiveUpgradeRequiredCheckerService extends UpgradeRequiredCheckerService with Auditor {

  override def upgradeRequired(deviceVersion: DeviceVersion)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] =
    withAudit("upgradeRequired", Map("os" -> deviceVersion.os.toString)) {
      ValidateAppVersion(deviceVersion)
    }
}

object LiveUpgradeRequiredCheckerService extends LiveUpgradeRequiredCheckerService{
  val auditConnector: AuditConnector = MicroserviceAuditConnector
}

object SandboxUpgradeRequiredCheckerService extends UpgradeRequiredCheckerService {
  override def upgradeRequired(deviceVersion: DeviceVersion)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] =
    Future.successful(false)
}
