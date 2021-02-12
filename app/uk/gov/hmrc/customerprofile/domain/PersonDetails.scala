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

import java.time.{LocalDate, ZoneOffset}

import play.api.libs.json.Json.format
import play.api.libs.json.Reads.DefaultLocalDateReads
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

/**
  * The dates coming from `citizen-details` are formatted strings, but we want to send
  * responses with numbers (millis-since-epoch), so we need an asymmetric json formatter.
  */
trait WriteDatesAsLongs {

  val dateWrites: Writes[LocalDate] = new Writes[LocalDate] {

    override def writes(o: LocalDate): JsValue =
      JsNumber(o.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli)
  }

  implicit val localDateFormat: Format[LocalDate] = new Format[LocalDate] {
    override def writes(o: LocalDate): JsValue = dateWrites.writes(o)

    override def reads(json: JsValue): JsResult[LocalDate] =
      DefaultLocalDateReads.reads(json)
  }
}

object Person extends WriteDatesAsLongs {
  implicit val formats: OFormat[Person] = format[Person]
}

case class Person(firstName: Option[String],
                  middleName: Option[String],
                  lastName: Option[String],
                  initials: Option[String],
                  title: Option[String],
                  honours: Option[String],
                  sex: Option[String],
                  dateOfBirth: Option[LocalDate],
                  nino: Option[Nino]) {

  lazy val shortName: Option[String] = for {
    f <- firstName
    l <- lastName
  } yield List(f, l).mkString(" ")

  lazy val fullName: String =
    List(title, firstName, middleName, lastName, honours).flatten.mkString(" ")
}

object Address extends WriteDatesAsLongs {
  implicit val formats: OFormat[Address] = format[Address]
}

case class Address(line1: Option[String],
                   line2: Option[String],
                   line3: Option[String],
                   line4: Option[String],
                   line5: Option[String],
                   postcode: Option[String],
                   country: Option[String],
                   startDate: Option[LocalDate],
                   `type`: Option[String])

object PersonDetails {
  implicit val formats: OFormat[PersonDetails] = format[PersonDetails]
}

case class PersonDetails(person: Person, address: Option[Address])
