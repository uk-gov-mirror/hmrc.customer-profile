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

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import uk.gov.hmrc.api.controllers.{ErrorGenericBadRequest, ErrorResponse, HeaderValidator}
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControlCheckOff, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.customerprofile.domain.DeviceVersion
import uk.gov.hmrc.customerprofile.services.{LiveUpgradeRequiredCheckerService, SandboxUpgradeRequiredCheckerService, UpgradeRequiredCheckerService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

sealed case class UpgradeRequired(upgrade : Boolean)
object UpgradeRequired {
  implicit val formats = Json.format[UpgradeRequired]
}

trait NativeVersionCheckerController extends BaseController with HeaderValidator with ErrorHandling {

  import DeviceVersion.formats
  import ErrorResponse.writes
  import UpgradeRequired.formats

  import scala.concurrent.ExecutionContext.Implicits.global

  val upgradeRequiredCheckerService : UpgradeRequiredCheckerService
  val accessControl: AccountAccessControlWithHeaderCheck

  final def validateAppVersion() = accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {

    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)

      request.body.validate[DeviceVersion].fold(
        errors => {
          Logger.warn("Received error with service validate app version: " + errors)
          Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
        },
        deviceVersion => {
          errorWrapper(upgradeRequiredCheckerService.upgradeRequired(deviceVersion).map {
            b => Ok(Json.toJson(new UpgradeRequired(b)))
          })
        }
      )
  }
}

object SandboxNativeVersionCheckerController extends NativeVersionCheckerController {
  val app = "Sandbox-Native-Version-Checker"
  override val accessControl = AccountAccessControlCheckOff
  override val upgradeRequiredCheckerService: UpgradeRequiredCheckerService = SandboxUpgradeRequiredCheckerService
}

object LiveNativeVersionCheckerController extends NativeVersionCheckerController {
  val app = "Live-Native-Version-Checker"
  override val accessControl = AccountAccessControlWithHeaderCheck
  override val upgradeRequiredCheckerService: UpgradeRequiredCheckerService = LiveUpgradeRequiredCheckerService
}
