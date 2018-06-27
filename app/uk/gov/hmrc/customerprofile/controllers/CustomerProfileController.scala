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

import com.google.inject.Inject
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, BodyParsers, Result}
import play.api.{Logger, LoggerLike, mvc}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.{AccountAccessControlCheckOff, AccountAccessControlWithHeaderCheck, NinoNotFoundOnAccount}
import uk.gov.hmrc.customerprofile.domain.Paperless
import uk.gov.hmrc.customerprofile.services.{CustomerProfileService, LiveCustomerProfileService, SandboxCustomerProfileService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait ErrorHandling {
  self: BaseController =>
  val app: String

  def log(message: String): Unit = Logger.info(s"$app $message")

  def result(errorResponse: ErrorResponse): Result =
    Status(errorResponse.httpStatusCode)(toJson(errorResponse))

  def errorWrapper(func: => Future[mvc.Result])(implicit hc: HeaderCarrier): Future[Result] = {
    func.recover {
      case e: AuthorisationException =>
        Unauthorized(Json.obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))

      case _: NotFoundException =>
        log("Resource not found!")
        result(ErrorNotFound)

      case _: NinoNotFoundOnAccount =>
        log("User has no NINO. Unauthorized!")
        Unauthorized(toJson(ErrorUnauthorizedNoNino))

      case e: Throwable =>
        Logger.error(s"$app Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(toJson(ErrorInternalServerError))
    }
  }
}

trait CustomerProfileController extends BaseController with HeaderValidator with ErrorHandling {

  val service: CustomerProfileService
  val accessControl: AccountAccessControlWithHeaderCheck

  final def getAccounts(journeyId: Option[String] = None): Action[AnyContent] =
    accessControl.validateAcceptWithoutAuth(acceptHeaderValidationRules).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper(
          service.getAccounts().map(
            as =>
              Ok(toJson(as))
          )
        )
    }

  def getLogger: LoggerLike = Logger

  final def getPersonalDetails(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] =
    accessControl.validateAcceptWithAuth(acceptHeaderValidationRules, Some(nino)).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper(
          service.getPersonalDetails(nino)
            .map(as => Ok(toJson(as)))
            .recover {
              case Upstream4xxResponse(_, LOCKED, _, _) =>
                result(ErrorManualCorrespondenceIndicator)
            }
        )
    }

  final def getPreferences(journeyId: Option[String] = None): Action[AnyContent] =
    accessControl.validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper(
          service.getPreferences().map {
            case Some(response) => Ok(toJson(response))
            case _ => NotFound
          }
        )
    }

  final def paperlessSettingsOptIn(journeyId: Option[String] = None): Action[JsValue] =
    accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        request.body.validate[Paperless].fold(
          errors => {
            Logger.warn("Received error with service getPaperlessSettings: " + errors)
            Future.successful(BadRequest(toJson(ErrorGenericBadRequest(errors))))
          },
          settings => {
            errorWrapper(service.paperlessSettings(settings).map {
              case PreferencesExists | EmailUpdateOk => Ok
              case PreferencesCreated => Created
              case EmailNotExist => Conflict(toJson(ErrorPreferenceConflict))
              case NoPreferenceExists => NotFound(toJson(ErrorNotFound))
              case _ => InternalServerError(toJson(PreferencesSettingsError))
            })
          }
        )
    }

  final def paperlessSettingsOptOut(journeyId: Option[String] = None): Action[AnyContent] =
    accessControl.validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper(service.paperlessSettingsOptOut().map {
          case PreferencesExists => Ok
          case PreferencesCreated => Created
          case PreferencesDoesNotExist => NotFound
          case PreferencesFailure => InternalServerError(toJson(PreferencesSettingsError))
        })
    }

}

class SandboxCustomerProfileController @Inject()(override val service: SandboxCustomerProfileService,
                                                 override val accessControl: AccountAccessControlCheckOff)
  extends CustomerProfileController {
  val app = "Sandbox-Customer-Profile"
}

class LiveCustomerProfileController @Inject()(override val service: LiveCustomerProfileService,
                                              override val accessControl: AccountAccessControlWithHeaderCheck)
  extends CustomerProfileController {
  val app = "Live-Customer-Profile"
}
