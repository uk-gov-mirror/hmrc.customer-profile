/*
 * Copyright 2018 HM Revenue & Customs
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

import net.ceedubs.ficus.readers.ValueReader
import play.api.libs.json.{JsError, Writes, _}
import uk.gov.hmrc.customerprofile.domain.NativeOS.Windows

import scala.concurrent.Future


case class NativeVersion(ios: VersionRange, android: VersionRange, windows: VersionRange)

trait LoadConfig {

  import com.typesafe.config.Config

  def config: Config
}

trait ApprovedAppVersions extends LoadConfig {
  import net.ceedubs.ficus.Ficus._

  private implicit val nativeVersionReader: ValueReader[NativeVersion] = ValueReader.relative { nativeVersion =>
    NativeVersion(
      VersionRange(config.as[String]("approvedAppVersions.ios")),
      VersionRange(config.as[String]("approvedAppVersions.android")),
      VersionRange(config.as[String]("approvedAppVersions.windows"))
    )
  }

  val appVersion: NativeVersion = config.as[NativeVersion]("approvedAppVersions")
}

trait ValidateAppVersion extends ApprovedAppVersions {

  import uk.gov.hmrc.customerprofile.domain.NativeOS.{iOS, Android}

  def upgrade(deviceVersion: DeviceVersion) : Future[Boolean] = {
    val outsideValidRange = deviceVersion.os match {
      case `iOS` => appVersion.ios.excluded(Version(deviceVersion.version))
      case Android => appVersion.android.excluded(Version(deviceVersion.version))
      case Windows => appVersion.windows.excluded(Version(deviceVersion.version))
    }
    Future.successful(outsideValidRange)
  }
}

object ValidateAppVersion extends ValidateAppVersion {
  import com.typesafe.config.{Config, ConfigFactory}
  
  lazy val config: Config = ConfigFactory.load()
}


trait NativeOS

object NativeOS {
  case object iOS extends NativeOS {
    override def toString: String = "ios"
  }
  case object Android extends NativeOS {
    override def toString: String = "android"
  }

  case object Windows extends NativeOS {
    override def toString: String = "windows"
  }

  val reads: Reads[NativeOS] = new Reads[NativeOS] {
    override def reads(json: JsValue): JsResult[NativeOS] = json match {
      case JsString("ios") => JsSuccess(iOS)
      case JsString("android") => JsSuccess(Android)
      case JsString("windows") => JsSuccess(Windows)
      case _ => JsError("unknown os")
    }
  }

  val writes: Writes[NativeOS] = new Writes[NativeOS] {
    override def writes(os: NativeOS) = os match {
      case `iOS` => JsString("ios")
      case Android => JsString("android")
      case Windows => JsString("windows")
    }
  }

  implicit val formats = Format(reads, writes)
}

case class DeviceVersion(os : NativeOS, version : String)

object DeviceVersion {
  import NativeOS.formats

  implicit val formats = Json.format[DeviceVersion]
}
