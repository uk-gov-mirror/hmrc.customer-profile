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
  language: String)

object Paperless {

  implicit val formats: OFormat[Paperless] = {
    import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}
    Json.format[Paperless]
  }
}

case class PaperlessOptOut(
  generic:  TermsAccepted,
  language: String)

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
  case object AndroidOptIn extends PageType
  case object iOSOptIn extends PageType
  case object AndroidOptOut extends PageType
  case object iOSOptOut extends PageType
  case object AndroidReOptIn extends PageType
  case object iOSReOptIn extends PageType

  val reads: Reads[PageType] = new Reads[PageType] {

    override def reads(json: JsValue): JsResult[PageType] = json match {
      case JsString("AndroidOptIn")   => JsSuccess(AndroidOptIn)
      case JsString("iOSOptIn")       => JsSuccess(iOSOptIn)
      case JsString("AndroidOptOut")  => JsSuccess(AndroidOptOut)
      case JsString("iOSOptOut")      => JsSuccess(iOSOptOut)
      case JsString("AndroidReOptIn") => JsSuccess(AndroidReOptIn)
      case JsString("iOSReOptIn")     => JsSuccess(iOSReOptIn)
      case _                          => JsError()
    }
  }

  val writes: Writes[PageType] = new Writes[PageType] {

    override def writes(pageType: PageType) = pageType match {
      case AndroidOptIn   => JsString("AndroidOptIn")
      case `iOSOptIn`     => JsString("iOSOptIn")
      case AndroidOptOut  => JsString("AndroidOptOut")
      case `iOSOptOut`    => JsString("iOSOptOut")
      case AndroidReOptIn => JsString("AndroidReOptIn")
      case `iOSReOptIn`   => JsString("iOSReOptIn")
    }
  }

  implicit val formats: Format[PageType] = Format(reads, writes)
}
