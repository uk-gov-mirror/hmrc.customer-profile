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

import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.customerprofile.connector.{AuthConnector, CitizenDetailsConnector}
import uk.gov.hmrc.customerprofile.controllers.action.AccountAccessControl

trait Profile extends BaseController {

  import uk.gov.hmrc.customerprofile.domain.CustomerProfile
  import CustomerProfile.formats
  import uk.gov.hmrc.domain.Nino
  import uk.gov.hmrc.customerprofile.domain.Accounts.accountsFmt

  import scala.concurrent.ExecutionContext.Implicits.global

  def authConnector: AuthConnector

  def citizenDetailsConnector: CitizenDetailsConnector

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
}

object Profile extends Profile {
  override val authConnector: AuthConnector = AuthConnector

  override val citizenDetailsConnector: CitizenDetailsConnector = CitizenDetailsConnector
}