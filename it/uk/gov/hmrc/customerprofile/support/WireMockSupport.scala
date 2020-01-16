/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.support

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{configureFor, reset}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

case class WireMockBaseUrl(value: URL)

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: Suite =>

  val wireMockPort:                               Int             = wireMockServer.port()
  val wireMockHost:                               String          = "localhost"
  val wireMockBaseUrlAsString:                    String          = s"http://$wireMockHost:$wireMockPort"
  val wireMockBaseUrl:                            URL             = new URL(wireMockBaseUrlAsString)
  protected implicit val implicitWireMockBaseUrl: WireMockBaseUrl = WireMockBaseUrl(wireMockBaseUrl)

  protected def basicWireMockConfig(): WireMockConfiguration = wireMockConfig()

  protected implicit lazy val wireMockServer: WireMockServer = {
    val server = new WireMockServer(basicWireMockConfig().dynamicPort())
    server.start()
    server
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }
}
