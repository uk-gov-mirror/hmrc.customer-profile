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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.emailaddress.EmailAddress

case class TermsAccepted(accepted: Boolean)

object TermsAccepted {
  implicit val formats: OFormat[TermsAccepted] = Json.format[TermsAccepted]
}

case class Paperless(
  generic: TermsAccepted,
  email:   EmailAddress)

object Paperless {

  implicit val formats: OFormat[Paperless] = {
    import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}
    Json.format[Paperless]
  }
}

case class PaperlessOptOut(generic: TermsAccepted)

object PaperlessOptOut {
  implicit val format: OFormat[PaperlessOptOut] = Json.format[PaperlessOptOut]
}
