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
import uk.gov.hmrc.customerprofile.connector._
import uk.gov.hmrc.customerprofile.controllers.action.AccountAccessControl
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

trait Profile extends BaseController {

  import play.api.libs.json.{JsError, Json}
  import play.api.mvc.BodyParsers
  import uk.gov.hmrc.customerprofile.domain.CustomerProfile
  import CustomerProfile.formats
  import uk.gov.hmrc.customerprofile.domain.Paperless
  import Paperless.formats
  import uk.gov.hmrc.customerprofile.domain.Accounts.accountsFmt
  import uk.gov.hmrc.domain.Nino

  import scala.concurrent.ExecutionContext.Implicits.global

  def authConnector: AuthConnector

  def citizenDetailsConnector: CitizenDetailsConnector

  def entityResolver : EntityResolverConnector

  def profile() = AccountAccessControl.async {
    implicit request =>
      CustomerProfile.create(authConnector.accounts, (nino) => citizenDetailsConnector.personDetails(nino.get)).map {
        cp =>
          Ok(Json.toJson(cp))
      } //TODO failure response
  }

  def accounts() = AccountAccessControl.async {
    implicit request =>
      authConnector.accounts() map (acc => Ok(Json.toJson(acc)))
  }

  def personalDetails(nino: Nino) = AccountAccessControl.async { implicit request =>
    citizenDetailsConnector.personDetails(nino).map {
      pd =>
        Ok(Json.toJson(pd))
    }
  }

  def paperlessSettings() = AccountAccessControl.async(BodyParsers.parse.json) {
    implicit request =>
      request.body.validate[Paperless].fold (
        errors => {
          Logger.warn("Received error" + errors)
          Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(errors))))
        },
        settings =>
          entityResolver.paperlessSettings(settings).map {
            case PreferencesExists => Ok(Json.toJson("The existing record has been updated"))
            case PreferencesCreated => Created(Json.toJson(""))
            case PreferencesFailure => InternalServerError("Unexpected error")
          }
      )
  }

//  def preferences() = AccountAccessControl.async {
//    implicit request =>
//      entityResolver.getPreferences().map{
//        maybePreference =>
//
//      }
//  }
}


object Profile extends Profile {
  override val authConnector: AuthConnector = AuthConnector

  override val citizenDetailsConnector: CitizenDetailsConnector = CitizenDetailsConnector

  override def entityResolver: EntityResolverConnector = EntityResolverConnector
}