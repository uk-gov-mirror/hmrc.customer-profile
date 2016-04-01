package uk.gov.hmrc.customerprofile.domain

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

object Person {
  implicit val formats = Json.format[Person]
}
case class Person(
                   firstName: Option[String],
                   middleName: Option[String],
                   lastName: Option[String],
                   initials: Option[String],
                   title: Option[String],
                   honours: Option[String],
                   sex: Option[String],
                   dateOfBirth: Option[DateTime],
                   nino: Option[Nino]
                 ) {

  lazy val shortName = for (f <- firstName; l <- lastName) yield List(f, l).mkString(" ")
  lazy val fullName = List(title, firstName, middleName, lastName, honours).flatten.mkString(" ")
}


object Address {
  implicit val formats = Json.format[Address]
}

case class Address(
                    line1: Option[String],
                    line2: Option[String],
                    line3: Option[String],
                    line4: Option[String],
                    postcode: Option[String],
                    startDate: Option[DateTime],
                    `type`: Option[String]
                  ) {
  lazy val lines = List(line1, line2, line3, line4).flatten
}


object PersonDetails {
  implicit val formats = Json.format[PersonDetails]
}
case class PersonDetails(
                          etag: String,
                          person: Person,
                          address: Option[Address],
                          correspondenceAddress: Option[Address]
                        )