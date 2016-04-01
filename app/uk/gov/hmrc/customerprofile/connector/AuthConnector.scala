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

package uk.gov.hmrc.customerprofile.connector

import play.api.Play
import uk.gov.hmrc.customerprofile.config.WSHttp
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

trait AuthConnector {

  import uk.gov.hmrc.customerprofile.domain.Accounts
  import uk.gov.hmrc.domain.{Nino, SaUtr}
  import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel

  val serviceUrl: String

  def http: HttpGet

  def confidenceLevel: ConfidenceLevel

  def accounts()(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[Option[Accounts]] = {
    http.GET(s"$serviceUrl/auth/authority") map {
      resp =>
        val json = resp.json
        val cl = (json \ "confidenceLevel").as[Int]
        if (cl >= confidenceLevel.level) {

          val accounts = json \ "accounts"

          val utr = (accounts \ "sa" \ "utr").asOpt[String]

          val nino = (accounts \ "paye" \ "nino").asOpt[String]

          val acc = Accounts(nino.map(Nino(_)), utr.map(SaUtr(_)))
          acc match {
            case Accounts(None, None) => None
            case _ => Some(acc)
          }
        } else
          None
    }
  }
}

object AuthConnector extends AuthConnector with ServicesConfig {
  import play.api.Play.current

  val serviceUrl = baseUrl("auth")
  val http = WSHttp
  val confidenceLevel: ConfidenceLevel = ConfidenceLevel.fromInt(Play.configuration.getInt("controllers.confidenceLevel")
    .getOrElse(throw new RuntimeException("The service has not been configured with a confidence level")))
}
