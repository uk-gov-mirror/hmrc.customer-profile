/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json._

case class PaperlessStatus(
  name:         StatusName,
  category:     Category,
  majorVersion: Option[Int] = None)

object PaperlessStatus {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]]{
    override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

    override def writes(o: Option[T]): JsValue = o match {
      case Some(t) ⇒ implicitly[Writes[T]].writes(t)
      case None ⇒ JsNull
    }
  }

  implicit val statusReads: Reads[PaperlessStatus] = (
    (__ \ "name").read[StatusName] and
      (__ \ "category").read[Category] and
      (__ \ "reoptinMajor").readNullable[Int]
    ) (PaperlessStatus.apply _)

  implicit val statusWrites: Writes[PaperlessStatus] = (
    (__ \ "name").write[StatusName] and
      (__ \ "category").write[Category] and
      (__ \ "majorVersion").writeNullable[Int]
    ) (unlift(PaperlessStatus.unapply))

}

sealed trait StatusName

object StatusName {

  case object Paper extends StatusName
  case object EmailNotVerified extends StatusName
  case object BouncedEmail extends StatusName
  case object Alright extends StatusName
  case object NewCustomer extends StatusName
  case object NoEmail extends StatusName
  case object Verified extends StatusName
  case object Bounced extends StatusName
  case object Pending extends StatusName
  case object ReOptIn extends StatusName

  val reads: Reads[StatusName] = new Reads[StatusName] {

    override def reads(json: JsValue): JsResult[StatusName] = json match {
      case JsString("PAPER")              => JsSuccess(Paper)
      case JsString("EMAIL_NOT_VERIFIED") => JsSuccess(Pending)
      case JsString("BOUNCED_EMAIL")      => JsSuccess(Bounced)
      case JsString("ALRIGHT")            => JsSuccess(Verified)
      case JsString("NEW_CUSTOMER")       => JsSuccess(NewCustomer)
      case JsString("NO_EMAIL")           => JsSuccess(NoEmail)
      case JsString("verified")           => JsSuccess(Verified)
      case JsString("bounced")            => JsSuccess(Bounced)
      case JsString("pending")            => JsSuccess(Pending)
      case JsString("OLD_VERSION")        => JsSuccess(ReOptIn)
      case _                              => JsError()
    }
  }

  val writes: Writes[StatusName] = new Writes[StatusName] {

    override def writes(statusName: StatusName): JsString = statusName match {
      case Paper       => JsString("Paper")
      case NewCustomer => JsString("NewCustomer")
      case NoEmail     => JsString("NoEmail")
      case Verified    => JsString("verified")
      case Bounced     => JsString("bounced")
      case Pending     => JsString("pending")
      case ReOptIn     => JsString("ReOptIn")
    }
  }

  implicit val formats: Format[StatusName] = Format(reads, writes)
}

sealed trait Category

object Category {
  import StatusName._

  case object ActionRequired extends Category
  case object Info extends Category
  case object ReOptInRequired extends Category
  case object OptInRequired extends Category

  private val statusByCategory: Map[Category, List[StatusName]] =
    Map(
      ActionRequired  -> List(NewCustomer, EmailNotVerified, BouncedEmail, NoEmail),
      Info            -> List(Alright),
      ReOptInRequired -> List(ReOptIn),
      OptInRequired   -> List(Paper)
    )

  private val categoryByStatus: Map[StatusName, Category] =
    for {
      (category, statuses) <- statusByCategory
      status               <- statuses
    } yield status -> category

  def apply(statusName: StatusName): Category = categoryByStatus(statusName)

  val reads: Reads[Category] = new Reads[Category] {

    override def reads(json: JsValue): JsResult[Category] = json match {
      case JsString("ACTION_REQUIRED")    => JsSuccess(ActionRequired)
      case JsString("INFO")               => JsSuccess(Info)
      case JsString("RE_OPT_IN_REQUIRED") => JsSuccess(ReOptInRequired)
      case JsString("OPT_IN_REQUIRED")    => JsSuccess(OptInRequired)
      case _                              => JsError()
    }
  }

  val writes: Writes[Category] = new Writes[Category] {

    override def writes(category: Category) = category match {
      case ActionRequired  => JsString("ActionRequired")
      case Info            => JsString("Info")
      case ReOptInRequired => JsString("ReOptInRequired")
      case OptInRequired   => JsString("OptInRequired")
    }
  }

  implicit val formats: Format[Category] = Format(reads, writes)
}
