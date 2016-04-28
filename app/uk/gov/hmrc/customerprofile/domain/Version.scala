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


case class VersionRange(lowerBound: Option[Version],
                        lowerBoundInclusive: Boolean,
                        upperBound: Option[Version],
                        upperBoundInclusive: Boolean,
                        qualifierStartsWith: Option[String] = None) {

  def excluded(version: Version): Boolean = !includes(version)

  def includes(version: Version): Boolean = {

    if (qualifierStartsWith.isDefined) {
      version.buildOrQualifier match {
        case Some(Right(q)) if q.toUpperCase.startsWith(qualifierStartsWith.get.toUpperCase) => true
        case _ => false
      }
    } else {

      val lbRange = lowerBound.fold(true)(lb => version.isAfter(lb) || (lowerBoundInclusive && lb.equals(version)))
      val ubRange = upperBound.fold(true)(ub => version.isBefore(ub) || (upperBoundInclusive && ub.equals(version)))
      lbRange && ubRange
    }
  }

  override def toString():String={
    if (qualifierStartsWith.isDefined) {
      s"[*-${qualifierStartsWith.get}]"
    } else {
      val start = if (lowerBoundInclusive) "[" else "("
      val end   = if (upperBoundInclusive) "]" else ")"

      start + lowerBound.map(_.toString).getOrElse("")  + "," + upperBound.map(_.toString).getOrElse("") + end
    }

  }
}

/**
  *
  * Supporting the following expressions:
  * Range               | Meaning
  * (,1.0.0]            | x <= 1.0.0
  * [1.0.0]             | Hard requirement on 1.0.0
  * [1.2.0,1.3.0]       | 1.2.0 <= x <= 1.3.0
  * [1.0.0,2.0.0)       | 1.0.0 <= x < 2.0.0
  * [1.5.0,)            | x >= 1.5.0
  * [*-SNAPSHOT]        | Any version with qualifier 'SNAPSHOT'
  *
  * All versions must have all 3 numbers, 1.0 is not supported for example
  *
  * @throws IllegalArgumentException when an illegal format is used
  */
object VersionRange {


  implicit def toVersion(v: String): Version = Version(v)

  val ValidFixedVersion = """^\[(\d+\.\d+.\d+)\]""".r
  val ValidVersionRangeLeftOpen = """^\(,?(\d+\.\d+.\d+)[\]\)]""".r
  val ValidVersionRangeRightOpen = """^[\[\(](\d+\.\d+.\d+),[\]\)]""".r
  val ValidVersionRangeBetween = """^[\[\(](\d+\.\d+.\d+),(\d+\.\d+.\d+)[\]\)]""".r
  val Qualifier = """^\[[-\*]+(.*)\]""".r

  def apply(range: String): VersionRange = {
    range.replaceAll(" ", "") match {
      case ValidFixedVersion(v) => VersionRange(Some(v), true, Some(v), true)
      case ValidVersionRangeLeftOpen(v) => VersionRange(None, false, Some(v), range.endsWith("]"))
      case ValidVersionRangeRightOpen(v) => VersionRange(Some(v), range.startsWith("["), None, false)
      case ValidVersionRangeBetween(v1, v2) => VersionRange(Some(v1), range.startsWith("["), Some(v2), range.endsWith("]"))
      case Qualifier(q) if q.length() > 1 => VersionRange(None, false, None, false, Some(q))
      case _ => throw new IllegalArgumentException(s"'$range' is not a valid range expression")
    }
  }
}


object Version {

  private def isAllDigits(x: String) = x forall Character.isDigit

  def apply(st: String): Version = {

    val split = st.split("[-_]", 2)
    val vv = toVer(split.lift(0).getOrElse("0"))
    val boq = toBoq(split.lift(1))

    if(vv == (0,0,0)) Version(0,0,0,Some(Right(st)))
    else Version(vv._1, vv._2, vv._3, boq)
  }

  def toVer(v: String):(Int, Int, Int) = {
    val elem = v.split('.')
    if (elem.forall(x => isAllDigits(x)) &&
      elem.size <= 3 &&
      !v.startsWith(".") &&
      !v.endsWith(".") &&
      !v.contains("..")
    ) (elem.lift(0).getOrElse("0").toInt, elem.lift(1).getOrElse("0").toInt, elem.lift(2).getOrElse("0").toInt)
    else (0,0,0)
  }
  def toBoq(boqStOpt: Option[String]): Option[Either[Long, String]] = boqStOpt.map(boqSt => if (isAllDigits(boqSt)) Left(boqSt.toLong) else Right(boqSt))

  def comparator(v1: Version, v2: Version): Boolean = v1.isAfter(v2)

  def isSnapshot(v: Version): Boolean = v.buildOrQualifier match {
    case Some(Right("SNAPSHOT")) => true
    case Some(Right(s)) if s.toLowerCase.startsWith("snap") => true
    case Some(Right(s)) if s.toLowerCase.startsWith("m") => true
    case _ => false
  }
}

case class Version(major: Int, minor: Int, revision: Int, buildOrQualifier: Option[Either[Long, String]] = None) extends Comparable[Version] {

  def isBefore(version: Version): Boolean = this.compareTo(version) < 0

  def isAfter(version: Version) = this.compareTo(version) > 0

  lazy val boqFormatted = buildOrQualifier.map { boqE => boqE match {
    case Left(num) => num.toString
    case Right(st) => st
  }}

  override def toString = s"$major.$minor.$revision${boqFormatted.map(b => "-" + b).getOrElse("")}"

  val parts = List(major, minor, revision)

  override def compareTo(version: Version): Int = {

    val partsComparison = parts.zip(version.parts).foldLeft(0) {
      case (result, (p1, p2)) => result match {
        case 0 => p1.compare(p2)
        case _ => result
      }
    }

    partsComparison match {
      case 0 => buildOrQualifier -> version.buildOrQualifier match {
        case (None, None) => 0
        case (None, Some(Left(b))) => -1
        case (None, Some(Right(q))) => 1
        case (Some(Left(b)), None) => 1
        case (Some(Right(q)), None) => -1
        case (Some(Left(b1)), Some(Right(s2))) => 1
        case (Some(Right(s1)), Some(Left(b2))) => -1
        case (Some(Left(b1)), Some(Left(b2))) => b1.compareTo(b2)
        case (Some(Right(s1)), Some(Right(s2))) => s1.compareTo(s2)
      }
      case _ => partsComparison
    }
  }


  override def equals(obj: scala.Any): Boolean = obj match {
    case v: Version => compareTo(v) == 0
    case _ => false
  }
}
