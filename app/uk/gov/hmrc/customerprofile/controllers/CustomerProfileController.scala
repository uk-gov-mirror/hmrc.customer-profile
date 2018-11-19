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

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession

import scala.concurrent.Future

trait CustomerProfileController extends HeaderValidator {
  def getAccounts(journeyId: Option[String] = None): Action[AnyContent]

  def getPersonalDetails(nino: Nino, journeyId: Option[String] = None): Action[AnyContent]

  def getPreferences(journeyId: Option[String] = None): Action[AnyContent]

  def paperlessSettingsOptOut(journeyId: Option[String] = None): Action[AnyContent]

  final def paperlessSettingsOptIn(journeyId: Option[String] = None): Action[JsValue] =
    withAcceptHeaderValidationAndAuthIfLive().async(BodyParsers.parse.json) {
      implicit request =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
        request.body.validate[Paperless].fold(
          errors => {
            Logger.warn("Received error with service getPaperlessSettings: " + errors)
            Future successful BadRequest
          },
          settings => {
            optIn(settings)
          }
        )
    }

  def withAcceptHeaderValidationAndAuthIfLive(taxId: Option[Nino] = None): ActionBuilder[Request]

  def optIn(settings: Paperless)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result]
}

