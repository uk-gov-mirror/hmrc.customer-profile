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

import play.api.mvc.{Action, BodyParsers}
import uk.gov.hmrc.customerprofile.connector.{EntityResolverConnector, PreferencesCreated, PreferencesExists, PreferencesFailure}
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControlForSandbox, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.customerprofile.domain.Paperless
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, LiveCustomerProfileService, SandboxCustomerProfileService}
import uk.gov.hmrc.domain.Nino
import play.api.{Logger, mvc}
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json._
import uk.gov.hmrc.api.controllers._

trait ErrorHandling {
  self: BaseController =>

  def errorWrapper(func: => Future[mvc.Result])(implicit hc: HeaderCarrier) = {
    func.recover {
      case ex: NotFoundException => Status(ErrorNotFound.httpStatusCode)(Json.toJson(ErrorNotFound))

      case ex: UnauthorizedException => Unauthorized(Json.toJson(ErrorUnauthorizedNoNino))

      case ex: ForbiddenException => Unauthorized(Json.toJson(ErrorUnauthorizedLowCL))

      case e: Throwable =>
        Logger.error(s"Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(Json.toJson(ErrorInternalServerError))
    }
  }
}

trait CustomerProfileController extends BaseController with HeaderValidator with ErrorHandling {

  import ErrorResponse.writes

  val service: CustomerProfileService
  val accessControl: AccountAccessControlWithHeaderCheck

  final def getAccounts = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)
      errorWrapper(service.getAccounts().map(as => Ok(Json.toJson(as))))
  }

  final def getPersonalDetails(nino: Nino) = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)
      errorWrapper(service.getPersonalDetails(nino).map(as => Ok(Json.toJson(as))))
  }

  final def getPreferences() = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)
      errorWrapper(service.getPreferences().map(as => Ok(Json.toJson(as))))
  }

  final def paperlessSettings() = accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {

    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)

      request.body.validate[Paperless].fold(
        errors => {
          Logger.warn("Received error with service getPaperlessSettings: " + errors)
          Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
        },
        settings => {
          errorWrapper(service.paperlessSettings(settings).map {
            case PreferencesExists => Ok(Json.toJson("The existing record has been updated"))
            case PreferencesCreated => Created(Json.toJson(""))
            // TODO: check server error status. Does not need to be a 500?
            case PreferencesFailure => InternalServerError(Json.toJson(PreferencesSettingsError))
          })
        }
      )
  }


}

object SandboxCustomerProfileController extends CustomerProfileController {
  override val service = SandboxCustomerProfileService
  override val accessControl = AccountAccessControlForSandbox
}

object LiveCustomerProfileController extends CustomerProfileController {
  override val service = LiveCustomerProfileService
  override val accessControl = AccountAccessControlWithHeaderCheck
}
