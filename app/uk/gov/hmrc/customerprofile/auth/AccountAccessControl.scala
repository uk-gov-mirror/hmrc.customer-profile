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

package uk.gov.hmrc.customerprofile.auth

import com.google.inject.Inject
import javax.inject.Named
import play.api.mvc.Results
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{nino, _}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.customerprofile.domain.Accounts
import uk.gov.hmrc.customerprofile.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case object ErrorUnauthorizedMicroService extends ErrorResponse(401, "UNAUTHORIZED", "Unauthorized to access resource")

class FailToMatchTaxIdOnAuth(message: String) extends HttpException(message, 403)

class NinoNotFoundOnAccount(message: String) extends HttpException(message, 403)

class AccountWithLowCL(message: String) extends HttpException(message, 403)

class AccountAccessControl @Inject() (
  val authConnector:                                                AuthConnector,
  val http:                                                         CoreGet,
  @Named("auth") val authUrl:                                       String,
  @Named("controllers.confidenceLevel") val serviceConfidenceLevel: Int)
    extends Results
    with AuthorisedFunctions {

  val ninoNotFoundOnAccount = new NinoNotFoundOnAccount("The user must have a National Insurance Number")

  def accounts(journeyId: JourneyId)(implicit hc: HeaderCarrier): Future[Accounts] =
    authorised()
      .retrieve(nino and saUtr and credentialStrength and confidenceLevel) {
        case foundNino ~ foundSaUtr ~ foundCredentialStrength ~ foundConfidenceLevel ⇒
          Future successful Accounts(
            foundNino.map(Nino(_)),
            foundSaUtr.map(SaUtr(_)),
            serviceConfidenceLevel > foundConfidenceLevel.level,
            foundCredentialStrength.orNull != "strong",
            journeyId.value
          )
      }

  def grantAccess(taxId: Option[Nino])(implicit hc: HeaderCarrier): Future[Unit] =
    authorised()
      .retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel ⇒
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          else if (taxId.nonEmpty && !taxId.get.value.equals(foundNino))
            throw new FailToMatchTaxIdOnAuth("The nino in the URL failed to match auth!")
          else if (serviceConfidenceLevel > foundConfidenceLevel.level)
            throw new AccountWithLowCL("The user does not have sufficient CL permissions to access this service")
          else Future.successful(())
        case None ~ _ ⇒
          throw ninoNotFoundOnAccount
      }
}
