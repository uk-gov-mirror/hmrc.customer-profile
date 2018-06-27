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

import uk.gov.hmrc.customerprofile.domain.DeviceVersion
import uk.gov.hmrc.customerprofile.domain.NativeOS.iOS

import scala.concurrent.ExecutionContext.Implicits.global

class UpgradeRequiredCheckerServiceSpec extends BaseServiceSpec {
  val service: LiveUpgradeRequiredCheckerService = new LiveUpgradeRequiredCheckerService(auditConnector, appNameConfiguration)

  "upgradeRequired" should {
    "audit and not require an upgrade for the configured lower bound version" in {
      mockAudit(transactionName = "upgradeRequired", Map("os" -> "ios"))
      await(service.upgradeRequired(DeviceVersion(iOS, "3.0.7"))) shouldBe false
    }

    "audit and not require an upgrade below the configured lower bound version" in {
      mockAudit(transactionName = "upgradeRequired", Map("os" -> "ios"))
      await(service.upgradeRequired(DeviceVersion(iOS, "3.0.6"))) shouldBe true
    }

    "audit and not require an upgrade above the configured lower bound version" in {
      mockAudit(transactionName = "upgradeRequired", Map("os" -> "ios"))
      await(service.upgradeRequired(DeviceVersion(iOS, "3.0.8"))) shouldBe false
    }
  }
}
