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

import com.google.inject.Singleton
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.customerprofile.domain.EmailPreference.Status.Verified
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future

@Singleton
class SandboxCustomerProfileController extends BaseController with CustomerProfileController with HeaderValidator {
  private val SANDBOX_CONTROL_HEADER = "SANDBOX-CONTROL"

  private val nino = Nino("CS700100A")

  private val personDetailsSandbox =
    PersonDetails(
      "etag",
      Person(Some("Jennifer"), None, Some("Thorsteinson"), None, Some("Ms"), None, Some("Female"), Some(org.joda.time.DateTime.parse("1999-01-31")), Some(nino)),
      Some(Address(Some("999 Big Street"), Some("Worthing"), Some("West Sussex"), None, None, Some("BN99 8IG"), None, None, None)))

  private def accounts(journeyId: Option[String]) =
    Accounts(Some(nino), None, routeToIV = false, routeToTwoFactor = false, journeyId.getOrElse(""))

  private val email = EmailAddress("name@email.co.uk")

  override def withAcceptHeaderValidationAndAuthIfLive(taxId: Option[Nino] = None): ActionBuilder[Request] =
    validateAccept(acceptHeaderValidationRules)

  override def getAccounts(journeyId: Option[String]): Action[AnyContent] = {
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-500") => InternalServerError
          case _ => Ok(toJson(accounts(journeyId)))
        })
    }
  }

  override def getPersonalDetails(nino: Nino, journeyId: Option[String]): Action[AnyContent] = {
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-500") => InternalServerError
          case _ => Ok(toJson(personDetailsSandbox))
        })
    }
  }

  override def getPreferences(journeyId: Option[String]): Action[AnyContent] = {
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-404") => NotFound
          case Some("ERROR-500") => InternalServerError
          case _ => Ok(toJson(Preference(digital = true, Some(EmailPreference(email, Verified)))))
        })
    }
  }

  override def paperlessSettingsOptOut(journeyId: Option[String]): Action[AnyContent] = {
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-404") => NotFound
          case Some("ERROR-500") => InternalServerError
          case Some("PREFERENCE-CREATED") => Created
          case _ => Ok
        })
    }
  }

  override def optIn(settings: Paperless)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    Future successful (request.headers.get(SANDBOX_CONTROL_HEADER) match {
      case Some("ERROR-401") => Unauthorized
      case Some("ERROR-403") => Forbidden
      case Some("ERROR-404") => NotFound
      case Some("ERROR-409") => Conflict
      case Some("ERROR-500") => InternalServerError
      case Some("PREFERENCE-CREATED") => Created
      case _ => Ok
    })
  }
}
