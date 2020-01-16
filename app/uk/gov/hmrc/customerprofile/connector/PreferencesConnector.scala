/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import play.api.libs.json.{Json, OFormat}
import play.api.{Configuration, Environment, Logger}
import play.mvc.Http.Status._
import uk.gov.hmrc.customerprofile.config.ServicesCircuitBreaker
import uk.gov.hmrc.customerprofile.domain.ChangeEmail
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

case class Entity(_id: String)

object Entity {
  implicit val format: OFormat[Entity] = Json.format[Entity]
}

@Singleton
class PreferencesConnector @Inject() (
  http:                             CorePut with CoreGet,
  @Named("preferences") serviceUrl: String,
  override val externalServiceName: String,
  val configuration:                Configuration,
  val environment:                  Environment)
    extends ServicesCircuitBreaker {

  def url(path: String): String = s"$serviceUrl$path"

  def updatePendingEmail(
    changeEmail: ChangeEmail,
    entityId:    String
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[PreferencesStatus] =
    http.PUT(url(s"/preferences/$entityId/pending-email"), changeEmail).map(_ => EmailUpdateOk).recoverWith {
      case e: NotFoundException ⇒ log(e.message, entityId); Future(NoPreferenceExists)
      case e: Upstream4xxResponse ⇒
        e.upstreamResponseCode match {
          case CONFLICT ⇒ log(e.message, entityId); Future(EmailNotExist)
          case NOT_FOUND ⇒ log(e.message, entityId); Future(NoPreferenceExists)
          case _ ⇒ log(e.message, entityId); Future(EmailUpdateFailed)
        }
      case _ ⇒ log("Failed to update preferences email", entityId); Future(EmailUpdateFailed)
    }

  def log(
    msg:      String,
    entityId: String
  ): Unit =
    Logger.warn(msg + s" for entity $entityId")
}
