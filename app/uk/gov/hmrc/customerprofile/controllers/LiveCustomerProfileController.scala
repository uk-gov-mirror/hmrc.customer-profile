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

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc._
import play.api.{Logger, LoggerLike, mvc}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.customerprofile.auth._
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.domain.Paperless
import uk.gov.hmrc.customerprofile.services.CustomerProfileService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream4xxResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class LiveCustomerProfileController @Inject()(service: CustomerProfileService,
                                              accessControl: AccountAccessControl,
                                              @Named("citizen-details.enabled") val citizenDetailsEnabled: Boolean) extends BaseController with CustomerProfileController {
  val app = "Live-Customer-Profile"

  def invokeAuthBlock[A](request: Request[A], block: Request[A] => Future[Result], taxId: Option[Nino]): Future[Result] = {
    implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)

    accessControl.grantAccess(taxId).flatMap { _ =>
      block(request)
    }.recover {
      case _: Upstream4xxResponse =>
        Logger.info("Unauthorized! Failed to grant access since 4xx response!")
        Unauthorized(toJson(ErrorUnauthorizedMicroService))

      case _: NinoNotFoundOnAccount =>
        Logger.info("Unauthorized! NINO not found on account!")
        Forbidden(toJson(ErrorUnauthorizedNoNino))

      case _: FailToMatchTaxIdOnAuth =>
        Logger.info("Unauthorized! Failure to match URL NINO against Auth NINO")
        Forbidden(toJson(ErrorUnauthorized))

      case _: AccountWithLowCL =>
        Logger.info("Unauthorized! Account with low CL!")
        Forbidden(toJson(ErrorUnauthorizedLowCL))

      case e: AuthorisationException =>
        Unauthorized(obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))
    }
  }

  override def withAcceptHeaderValidationAndAuthIfLive(taxId: Option[Nino] = None): ActionBuilder[Request] = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
      if (acceptHeaderValidationRules(request.headers.get("Accept"))) {
        invokeAuthBlock(request, block, taxId)
      }
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
  }

  def log(message: String): Unit = Logger.info(s"$app $message")

  def result(errorResponse: ErrorResponse): Result =
    Status(errorResponse.httpStatusCode)(toJson(errorResponse))

  def errorWrapper(func: => Future[mvc.Result])(implicit hc: HeaderCarrier): Future[Result] = {
    func.recover {
      case e: AuthorisationException =>
        Unauthorized(obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))

      case _: NotFoundException =>
        log("Resource not found!")
        result(ErrorNotFound)

      case _: NinoNotFoundOnAccount =>
        log("User has no NINO. Unauthorized!")
        Forbidden(toJson(ErrorUnauthorizedNoNino))

      case e: Throwable =>
        Logger.error(s"$app Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(toJson(ErrorInternalServerError))
    }
  }

  override def getAccounts(journeyId: Option[String] = None): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)

        errorWrapper(
          service.getAccounts().map(
            as => Ok(toJson(as))
          )
        )
    }

  def getLogger: LoggerLike = Logger

  override def getPersonalDetails(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive(Some(nino)).async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper {
          if (citizenDetailsEnabled) {
            service.getPersonalDetails(nino)
              .map(as => Ok(toJson(as)))
              .recover {
                case Upstream4xxResponse(_, LOCKED, _, _) =>
                  result(ErrorManualCorrespondenceIndicator)
              }
          } else Future successful result(ErrorNotFound)
        }
    }

  override def getPreferences(journeyId: Option[String] = None): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive().async {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        errorWrapper(
          service.getPreferences().map {
            case Some(response) => Ok(toJson(response))
            case _ => NotFound
          }
        )
    }

  override def optIn(settings: Paperless)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    errorWrapper(service.paperlessSettings(settings).map {
      case PreferencesExists | EmailUpdateOk => Ok
      case PreferencesCreated => Created
      case EmailNotExist => Conflict(toJson(ErrorPreferenceConflict))
      case NoPreferenceExists => NotFound(toJson(ErrorNotFound))
      case _ => InternalServerError(toJson(PreferencesSettingsError))
    })
  }

  override def paperlessSettingsOptOut(journeyId: Option[String] = None): Action[AnyContent] =
    withAcceptHeaderValidationAndAuthIfLive().async {
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
