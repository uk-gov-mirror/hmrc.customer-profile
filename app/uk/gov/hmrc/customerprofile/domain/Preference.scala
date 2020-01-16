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

package uk.gov.hmrc.customerprofile.domain

import play.api.libs.json.{JsError, Json, _}
import uk.gov.hmrc.emailaddress.EmailAddress

case class EmailPreference(
  email:  EmailAddress,
  status: EmailPreference.Status)

object EmailPreference {

  import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}

  sealed trait Status

  object Status {
    case object Pending extends Status
    case object Bounced extends Status
    case object Verified extends Status

    val reads: Reads[Status] = new Reads[Status] {

      override def reads(json: JsValue): JsResult[Status] = json match {
        case JsString("pending")  => JsSuccess(Pending)
        case JsString("bounced")  => JsSuccess(Bounced)
        case JsString("verified") => JsSuccess(Verified)
        case _                    => JsError()
      }
    }

    val writes: Writes[Status] = new Writes[Status] {

      override def writes(status: Status) = status match {
        case Pending  => JsString("pending")
        case Bounced  => JsString("bounced")
        case Verified => JsString("verified")
      }
    }

    implicit val formats: Format[Status] = Format(reads, writes)
  }

  implicit val formats: OFormat[EmailPreference] = Json.format[EmailPreference]
}

case class Preference(
  digital: Boolean,
  email:   Option[EmailPreference] = None)

object Preference {

  implicit val format: OFormat[Preference] = {
    Json.format[Preference]
  }
}
