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

package uk.gov.hmrc.customerprofile.connector


import play.api.Logger
import play.api.libs.json.Json
import play.mvc.Http.Status._
import uk.gov.hmrc.customerprofile.config.{ServicesCircuitBreaker, WSHttp}
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


case class Entity(_id: String)

object Entity {
  implicit val format = Json.format[Entity]
}

trait PreferencesConnector {

  self: ServicesCircuitBreaker ⇒
  def serviceUrl: String
  def http : HttpPut
  def url(path: String) : String = s"$serviceUrl$path"

  def updatePendingEmail(changeEmail: ChangeEmail, entityId: String)(implicit hc: HeaderCarrier, ex : ExecutionContext): Future[PreferencesStatus] = {
    http.PUT(url(s"/preferences/${entityId}/pending-email"), changeEmail).map(_ ⇒ EmailUpdateOk).recoverWith {
      case e: NotFoundException ⇒ log(e.message, entityId); Future(NoPreferenceExists)
      case e: Upstream4xxResponse ⇒ {
        e.upstreamResponseCode match {
          case CONFLICT ⇒ log(e.message, entityId); Future(EmailNotExist)
          case NOT_FOUND ⇒ log(e.message, entityId); Future(NoPreferenceExists)
          case _ ⇒ log(e.message, entityId); Future(EmailUpdateFailed)
        }
      }
      case _ ⇒ log("Failed to update preferences email", entityId); Future(EmailUpdateFailed)
    }
  }

  def log(msg: String, entityId: String) = {
    Logger.warn(msg + s" for entity $entityId")
  }
}

object PreferencesConnector extends PreferencesConnector with ServicesConfig with ServicesCircuitBreaker {
  override def http: HttpPut with CoreGet = WSHttp
  override protected val externalServiceName: String = "preferences"
  override def serviceUrl: String = baseUrl(externalServiceName)
}