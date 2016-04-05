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

package uk.gov.hmrc.customerprofile.controllers.action

import play.api.mvc.{ActionBuilder, Request, Result}
import uk.gov.hmrc.customerprofile.connector.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


trait AccountAccessControl extends ActionBuilder[Request] {

  import scala.concurrent.ExecutionContext.Implicits.global

  val authConnector: AuthConnector

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
    authConnector.hasNino().flatMap {
      _ =>
        block(request)
    }
  }

}

object AccountAccessControl extends AccountAccessControl {
  val authConnector: AuthConnector = AuthConnector
}
