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
import uk.gov.hmrc.customerprofile.domain.Language.English
import uk.gov.hmrc.emailaddress.EmailAddress

case class TermsAccepted(
  accepted:  Option[Boolean],
  optInPage: Option[OptInPage] = None)

object TermsAccepted {
  implicit val formats: OFormat[TermsAccepted] = Json.format[TermsAccepted]
}

case class Paperless(
  generic:  TermsAccepted,
  email:    EmailAddress,
  language: Option[Language] = Some(English))

object Paperless {

  implicit val formats: OFormat[Paperless] = {
    import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}
    Json.using[Json.WithDefaultValues].format[Paperless]
  }
}

case class PaperlessOptOut(
  generic:  Option[TermsAccepted],
  language: Option[Language] = Some(English))

object PaperlessOptOut {
  implicit val format: OFormat[PaperlessOptOut] = Json.using[Json.WithDefaultValues].format[PaperlessOptOut]
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
  case object IosOptInPage extends PageType
  case object AndroidOptOutPage extends PageType
  case object IosOptOutPage extends PageType
  case object AndroidReOptInPage extends PageType
  case object IosReOptInPage extends PageType
  case object AndroidReOptOutPage extends PageType
  case object IosReOptOutPage extends PageType

  val reads: Reads[PageType] = new Reads[PageType] {

    override def reads(json: JsValue): JsResult[PageType] = json match {
      case JsString("AndroidOptInPage")    => JsSuccess(AndroidOptInPage)
      case JsString("IosOptInPage")        => JsSuccess(IosOptInPage)
      case JsString("AndroidOptOutPage")   => JsSuccess(AndroidOptOutPage)
      case JsString("IosOptOutPage")       => JsSuccess(IosOptOutPage)
      case JsString("AndroidReOptInPage")  => JsSuccess(AndroidReOptInPage)
      case JsString("IosReOptInPage")      => JsSuccess(IosReOptInPage)
      case JsString("AndroidReOptOutPage") => JsSuccess(AndroidReOptOutPage)
      case JsString("IosReOptOutPage")     => JsSuccess(IosReOptOutPage)
      case _                               => JsError()
    }
  }

  val writes: Writes[PageType] = new Writes[PageType] {

    override def writes(pageType: PageType) = pageType match {
      case AndroidOptInPage    => JsString("AndroidOptInPage")
      case IosOptInPage        => JsString("IosOptInPage")
      case AndroidOptOutPage   => JsString("AndroidOptOutPage")
      case IosOptOutPage       => JsString("IosOptOutPage")
      case AndroidReOptInPage  => JsString("AndroidReOptInPage")
      case IosReOptInPage      => JsString("IosReOptInPage")
      case AndroidReOptOutPage => JsString("AndroidReOptOutPage")
      case IosReOptOutPage     => JsString("IosReOptOutPage")
    }
  }

  implicit val formats: Format[PageType] = Format(reads, writes)
}

sealed trait Language

object Language {
  case object English extends Language

  val reads: Reads[Language] = new Reads[Language] {

    override def reads(json: JsValue): JsResult[Language] = json match {
      case JsString("en") => JsSuccess(English)
      case _              => JsError()
    }
  }

  val writes: Writes[Language] = new Writes[Language] {

    override def writes(language: Language): JsString = language match {
      case English => JsString("en")
    }
  }

  implicit val formats: Format[Language] = Format(reads, writes)
}
