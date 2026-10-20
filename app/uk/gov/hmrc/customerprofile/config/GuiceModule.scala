/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.config

import com.google.inject.AbstractModule
import com.google.inject.name.Names.named
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  val servicesConfig = new ServicesConfig(configuration)

  override def configure(): Unit = {
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])

    bindConfigInt("controllers.confidenceLevel")
    bindConfigInt("mongodb.ttlDays")
    bindConfigInt("service.maxStoredPins")
    bind(classOf[String]).annotatedWith(named("api-access")).toInstance("INTERNAL")

    bindConfigBoolean("citizen-details.enabled", "microservice.services.citizen-details.enabled")
    bindConfigBoolean("optInVersionsEnabled", "optInVersionsEnabled")
    bindConfigBoolean("reOptInEnabled", "reOptInEnabled")
    bindConfigString("key", "googlePass.key")
    bindConfigString("dobErrorKey", "dobErrorKey")
    bindConfigString("previousPinErrorKey", "previousPinErrorKey")

    bind(classOf[String]).annotatedWith(named("auth")).toInstance(servicesConfig.baseUrl("auth"))
    bind(classOf[String]).annotatedWith(named("citizen-details")).toInstance(servicesConfig.baseUrl("citizen-details"))
    bind(classOf[String]).annotatedWith(named("entity-resolver")).toInstance(servicesConfig.baseUrl("entity-resolver"))
    bind(classOf[String]).annotatedWith(named("preferences")).toInstance(servicesConfig.baseUrl("preferences"))
    bind(classOf[String])
      .annotatedWith(named("mobile-shuttering"))
      .toInstance(servicesConfig.baseUrl("mobile-shuttering"))
    bind(classOf[String])
      .annotatedWith(named("find-my-nino-add-to-wallet"))
      .toInstance(servicesConfig.baseUrl("find-my-nino-add-to-wallet"))
  }

  /** Binds a configuration value using the `path` as the name for the binding. Throws an exception if the configuration value does not exist or
    * cannot be read as an Int.
    */
  private def bindConfigInt(path: String): Unit =
    bindConstant()
      .annotatedWith(named(path))
      .to(configuration.underlying.getInt(path))

  private def bindConfigBoolean(
    name: String,
    path: String
  ): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getBoolean(path))

  private def bindConfigString(
    name: String,
    path: String
  ): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getString(path))
}
