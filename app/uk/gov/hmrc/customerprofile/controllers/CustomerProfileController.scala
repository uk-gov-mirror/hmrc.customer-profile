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

package uk.gov.hmrc.customerprofile.controllers

import play.api.libs.json._
import play.api.mvc.{BodyParsers, Result}
import play.api.{Logger, LoggerLike, mvc}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControlCheckOff, AccountAccessControlWithHeaderCheck, NinoNotFoundOnAccount}
import uk.gov.hmrc.customerprofile.domain.Paperless
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, LiveCustomerProfileService, SandboxCustomerProfileService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

trait ErrorHandling {
  self: BaseController =>
  val app:String

  def log(message:String) = Logger.info(s"$app $message")

  def result(errorResponse: ErrorResponse): Result =
    Status(errorResponse.httpStatusCode)(Json.toJson(errorResponse))

  def errorWrapper(func: => Future[mvc.Result])(implicit hc: HeaderCarrier) = {
    func.recover {
      case ex: NotFoundException =>
        log("Resource not found!")
        result(ErrorNotFound)

      case ex: NinoNotFoundOnAccount =>
        log("User has no NINO. Unauthorized!")
        Unauthorized(Json.toJson(ErrorUnauthorizedNoNino))

      case e: Throwable =>
        Logger.error(s"$app Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(Json.toJson(ErrorInternalServerError))
    }
  }
}

trait CustomerProfileController extends BaseController with HeaderValidator with ErrorHandling {

  val service: CustomerProfileService
  val accessControl: AccountAccessControlWithHeaderCheck

  final def getAccounts(journeyId: Option[String] = None) = AccountAccessControlCheckOff.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)
      errorWrapper(service.getAccounts().map(as => Ok(Json.toJson(as))))
  }

  def getLogger: LoggerLike = Logger

  final def getPersonalDetails(nino: Nino, journeyId: Option[String] = None) = accessControl.validateAcceptWithAuth(acceptHeaderValidationRules, Some(nino)).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)
      errorWrapper(
        service.getPersonalDetails(nino)
          .map(as => Ok(Json.toJson(as)))
          .recover {
            case Upstream4xxResponse(_, LOCKED, _, _) =>
              result(ErrorManualCorrespondenceIndicator)
          }
      )
  }

  final def getPreferences(journeyId: Option[String] = None) = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)
      errorWrapper(
        service.getPreferences().map {
          case Some(response) => Ok(Json.toJson(response))
          case _ => NotFound
        }
      )
  }

  final def paperlessSettingsOptIn(journeyId: Option[String] = None) = accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {
    implicit request ⇒
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)
      request.body.validate[Paperless].fold(
        errors ⇒ {
          Logger.warn("Received error with service getPaperlessSettings: " + errors)
          Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
        },
        settings => {
          errorWrapper(service.paperlessSettings(settings).map {
            case PreferencesExists | EmailUpdateOk ⇒ Ok
            case PreferencesCreated ⇒ Created
            case EmailNotExist ⇒ Conflict(Json.toJson(ErrorPreferenceConflict))
            case NoPreferenceExists ⇒ NotFound(Json.toJson(ErrorNotFound))
            case _ ⇒ InternalServerError(Json.toJson(PreferencesSettingsError))
          })
        }
      )
  }

  final def paperlessSettingsOptOut(journeyId: Option[String] = None) = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)
      errorWrapper(service.paperlessSettingsOptOut().map {
        case PreferencesExists => Ok
        case PreferencesCreated => Created
        case PreferencesDoesNotExist => NotFound
        case PreferencesFailure => InternalServerError(Json.toJson(PreferencesSettingsError))
      })
  }

}

object SandboxCustomerProfileController extends CustomerProfileController {
  val app = "Sandbox-Customer-Profile"
  override val service = SandboxCustomerProfileService
  override val accessControl = AccountAccessControlCheckOff
}

object LiveCustomerProfileController extends CustomerProfileController {
  val app = "Live-Customer-Profile"
  override val service = LiveCustomerProfileService
  override val accessControl = AccountAccessControlWithHeaderCheck
}
