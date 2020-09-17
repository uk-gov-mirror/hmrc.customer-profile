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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.emailaddress.EmailAddress

case class TermsAccepted(
  accepted:  Boolean,
  optInPage: Option[OptInPage] = None)

object TermsAccepted {
  implicit val formats: OFormat[TermsAccepted] = Json.format[TermsAccepted]
}

case class Paperless(
  generic:  TermsAccepted,
  email:    EmailAddress,
  language: Option[String])

object Paperless {

  implicit val formats: OFormat[Paperless] = {
    import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}
    Json.format[Paperless]
  }
}

case class PaperlessOptOut(
  generic:  TermsAccepted,
  language: Option[String])

object PaperlessOptOut {
  implicit val format: OFormat[PaperlessOptOut] = Json.format[PaperlessOptOut]
}

case class OptInPage(
  version:  Version,
  cohort:   Int,
  pageType: PageType)

object OptInPage {
  implicit val format: OFormat[OptInPage] = Json.format[OptInPage]
}

case class Version(
  major: Int,
  minor: Int)

object Version {
  implicit val format: OFormat[Version] = Json.format[Version]
}

sealed trait PageType

object PageType {
  case object AndroidOptInPage extends PageType
  case object iOSOptInPage extends PageType
  case object AndroidOptOutPage extends PageType
  case object iOSOptOutPage extends PageType
  case object AndroidReOptInPage extends PageType
  case object iOSReOptInPage extends PageType

  val reads: Reads[PageType] = new Reads[PageType] {

    override def reads(json: JsValue): JsResult[PageType] = json match {
      case JsString("AndroidOptInPage")   => JsSuccess(AndroidOptInPage)
      case JsString("iOSOptInPage")       => JsSuccess(iOSOptInPage)
      case JsString("AndroidOptOutPage")  => JsSuccess(AndroidOptOutPage)
      case JsString("iOSOptOutPage")      => JsSuccess(iOSOptOutPage)
      case JsString("AndroidReOptInPage") => JsSuccess(AndroidReOptInPage)
      case JsString("iOSReOptInPage")     => JsSuccess(iOSReOptInPage)
      case _                          => JsError()
    }
  }

  val writes: Writes[PageType] = new Writes[PageType] {

    override def writes(pageType: PageType) = pageType match {
      case AndroidOptInPage   => JsString("AndroidOptInPage")
      case `iOSOptInPage`     => JsString("iOSOptInPage")
      case AndroidOptOutPage  => JsString("AndroidOptOutPage")
      case `iOSOptOutPage`    => JsString("iOSOptOutPage")
      case AndroidReOptInPage => JsString("AndroidReOptInPage")
      case `iOSReOptInPage`   => JsString("iOSReOptInPage")
    }
  }

  implicit val formats: Format[PageType] = Format(reads, writes)
}
