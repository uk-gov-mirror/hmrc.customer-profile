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

package uk.gov.hmrc.customerprofile.domain

import play.api.libs.json.{JsError, JsSuccess, Writes, _}
import uk.gov.hmrc.emailaddress.EmailAddress

case class TermsAccepted(accepted: Boolean)

object TermsAccepted {
  implicit val formats = Json.format[TermsAccepted]
}

case class Paperless(generic: TermsAccepted, email: EmailAddress)

object Paperless {
  implicit val formats = {
    import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}
    Json.format[Paperless]
  }
}

case class PaperlessOptOut(deEnrolling : Boolean, reason : Option[String] = None)

object PaperlessOptOut {

  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._

  private val reads: Reads[PaperlessOptOut] = (
      (JsPath \ "de-enrolling").read[Boolean] and
      (JsPath \ "reason").read[Option[String]]
    )(PaperlessOptOut.apply _)

  private val writes: Writes[PaperlessOptOut] = (
      (JsPath \ "de-enrolling").write[Boolean] and
      (JsPath \ "reason").write[Option[String]]
    )(unlift(PaperlessOptOut.unapply))

  implicit val formats = Format(reads, writes)
}