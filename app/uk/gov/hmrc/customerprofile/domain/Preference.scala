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

import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.emailaddress.EmailAddress

case class EmailPreference(
  email:    EmailAddress,
  status:   StatusName,
  linkSent: Option[LocalDate] = None)

object EmailPreference {

  import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}

  implicit val localdateFormatDefault = new Format[LocalDate] {
    override def reads(json: JsValue):   JsResult[LocalDate] = JodaReads.DefaultJodaLocalDateReads.reads(json)
    override def writes(o:   LocalDate): JsValue             = JodaWrites.DefaultJodaLocalDateWrites.writes(o)
  }

  implicit val formats: OFormat[EmailPreference] = Json.format[EmailPreference]
}

case class Preference(
  digital:      Boolean,
  emailAddress: Option[String] = None,
  linkSent:     Option[LocalDate] = None,
  email:        Option[EmailPreference] = None,
  status:       Option[PaperlessStatus] = None)

object Preference {

  implicit val localdateFormatDefault: Format[LocalDate] = new Format[LocalDate] {
    override def reads(json: JsValue):   JsResult[LocalDate] = JodaReads.DefaultJodaLocalDateReads.reads(json)
    override def writes(o:   LocalDate): JsValue             = JodaWrites.DefaultJodaLocalDateWrites.writes(o)
  }

  implicit val format: OFormat[Preference] = {
    Json.format[Preference]
  }
}
