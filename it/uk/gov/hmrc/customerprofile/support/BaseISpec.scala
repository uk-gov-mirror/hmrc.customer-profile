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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.language.postfixOps

class BaseISpec
    extends WordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout {
  override implicit lazy val app: Application = appBuilder
    .build()

  def config: Map[String, Any] = Map(
    "auditing.enabled"                              -> false,
    "microservice.services.auth.port"               -> wireMockPort,
    "microservice.services.citizen-details.port"    -> wireMockPort,
    "microservice.services.entity-resolver.port"    -> wireMockPort,
    "microservice.services.preferences.port"        -> wireMockPort,
    "play.ws.timeout.connection"                    -> "6000 seconds",
    "play.ws.timeout.request"                       -> "20000 seconds"
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
