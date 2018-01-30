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

import uk.gov.hmrc.api.controllers.ErrorResponse
import play.api.http.Status._

case object ErrorNinoInvalid extends ErrorResponse(BAD_REQUEST, "NINO_INVALID", "The provided NINO is invalid")

case object ErrorUnauthorizedNoNino extends ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", "NINO does not exist on account")

case object ErrorUnauthorizedMicroService extends ErrorResponse(UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized to access resource")

case object ErrorUnauthorizedWeakCredStrength extends ErrorResponse(UNAUTHORIZED, "WEAK_CRED_STRENGTH", "Credential Strength on account does not allow access")

case object ErrorManualCorrespondenceIndicator extends ErrorResponse(LOCKED, "MANUAL_CORRESPONDENCE_IND", "Data cannot be disclosed to the user because MCI flag is set in NPS")

case object ErrorPreferenceConflict extends ErrorResponse(CONFLICT, "CONFLICT", "No existing verified or pending data")
