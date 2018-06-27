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

package uk.gov.hmrc.customerprofile.controllers.action

import java.util.UUID.randomUUID

import com.google.inject.Inject
import javax.inject.Named
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.customerprofile.controllers.ErrorUnauthorizedNoNino
import uk.gov.hmrc.customerprofile.domain.Accounts
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case object ErrorUnauthorizedMicroService extends ErrorResponse(401, "UNAUTHORIZED", "Unauthorized to access resource")

class FailToMatchTaxIdOnAuth(message: String) extends HttpException(message, 401)

class NinoNotFoundOnAccount(message: String) extends HttpException(message, 401)

class AccountWithLowCL(message: String) extends HttpException(message, 401)

class AccountAccessControl @Inject()(val authConnector: AuthConnector,
                                     val http: CoreGet,
                                     @Named("auth") val authUrl: String,
                                     @Named("controllers.confidenceLevel") val serviceConfidenceLevel: Int) extends Results with AuthorisedFunctions {

  val ninoNotFoundOnAccount = new NinoNotFoundOnAccount("The user must have a National Insurance Number")

  def accounts(implicit hc: HeaderCarrier): Future[Accounts] = {
    authorised()
      .retrieve(nino and saUtr and credentialStrength and confidenceLevel) {
        case nino ~ saUtr ~ credentialStrength ~ confidenceLevel ⇒ {
          Future successful Accounts(
            nino.map(Nino(_)),
            saUtr.map(SaUtr(_)),
            serviceConfidenceLevel > confidenceLevel.level,
            credentialStrength.orNull != "strong",
            journeyId = randomUUID().toString)
        }
      }
  }

  def invokeAuthBlock[A](request: Request[A], block: (Request[A]) => Future[Result], taxId: Option[Nino]): Future[Result] = {
    implicit val hc = fromHeadersAndSession(request.headers, None)

    grantAccess(taxId).flatMap { access =>
      block(request)
    }.recover {
      case _: Upstream4xxResponse =>
        Logger.info("Unauthorized! Failed to grant access since 4xx response!")
        Unauthorized(toJson(ErrorUnauthorizedMicroService))

      case _: NinoNotFoundOnAccount =>
        Logger.info("Unauthorized! NINO not found on account!")
        Unauthorized(toJson(ErrorUnauthorizedNoNino))

      case _: FailToMatchTaxIdOnAuth =>
        Logger.info("Unauthorized! Failure to match URL NINO against Auth NINO")
        Status(ErrorUnauthorized.httpStatusCode)(toJson(ErrorUnauthorized))

      case _: AccountWithLowCL =>
        Logger.info("Unauthorized! Account with low CL!")
        Unauthorized(toJson(ErrorUnauthorizedLowCL))

      case e: AuthorisationException =>
        Unauthorized(Json.obj("httpStatusCode" -> 401, "errorCode" -> "UNAUTHORIZED", "message" -> e.getMessage))
    }
  }

  def grantAccess(taxId: Option[Nino])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    authorised()
      .retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel ⇒ {
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          else if (taxId.nonEmpty && !taxId.get.value.equals(foundNino))
            throw new FailToMatchTaxIdOnAuth("The nino in the URL failed to match auth!")
          else if (serviceConfidenceLevel > foundConfidenceLevel.level)
            throw new AccountWithLowCL("The user does not have sufficient CL permissions to access this service")
          else Future(Unit)
        }
        case None ~ _ ⇒ {
          throw ninoNotFoundOnAccount
        }
      }
  }

  def validateAcceptWithoutAuth(rules: Option[String] => Boolean) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      if (rules(request.headers.get("Accept"))) block(request)
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
  }
}

class AccountAccessControlWithHeaderCheck @Inject()(val accessControl: AccountAccessControl) extends HeaderValidator {
  val checkAccess = true

  def validateAcceptWithoutAuth(rules: Option[String] => Boolean) = accessControl.validateAcceptWithoutAuth(rules)

  def validateAcceptWithAuth(rules: Option[String] => Boolean, taxId: Option[Nino]) = new ActionBuilder[Request] {

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      if (rules(request.headers.get("Accept"))) {
        if (checkAccess) accessControl.invokeAuthBlock(request, block, taxId)
        else block(request)
      }
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
  }

  override def validateAccept(rules: Option[String] => Boolean) = new ActionBuilder[Request] {

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      if (rules(request.headers.get("Accept"))) {
        if (checkAccess) accessControl.invokeAuthBlock(request, block, None)
        else block(request)
      }
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
  }
}

class AccountAccessControlOff @Inject()(@Named("auth") override val authUrl: String,
                                        override val http: HttpGet,
                                        override val authConnector: AuthConnector,
                                        override val serviceConfidenceLevel: Int = 0)
  extends AccountAccessControl(authConnector, http, authUrl, serviceConfidenceLevel)

class AccountAccessControlCheckOff @Inject()(override val accessControl: AccountAccessControl)
  extends AccountAccessControlWithHeaderCheck(accessControl) {
  override val checkAccess = false
}
