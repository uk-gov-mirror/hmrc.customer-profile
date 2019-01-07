/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.name.Named
import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, Provides}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.api.connector.{ApiServiceLocatorConnector, ServiceLocatorConnector}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customerprofile.tasks.ServiceLocatorRegistrationTask
import uk.gov.hmrc.http.{CoreGet, CorePost, CorePut}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.customerprofile.controllers.api.ApiAccess
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

import scala.collection.JavaConverters._

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule with ServicesConfig {

  override protected lazy val mode: Mode = environment.mode
  override protected lazy val runModeConfiguration: Configuration = configuration

  override def configure(): Unit = {

    bind(classOf[ServiceLocatorConnector]).to(classOf[ApiServiceLocatorConnector])
    bind(classOf[CoreGet]).to(classOf[WSHttpImpl])
    bind(classOf[CorePut]).to(classOf[WSHttpImpl])
    bind(classOf[CorePost]).to(classOf[WSHttpImpl])
    bind(classOf[HttpClient]).to(classOf[WSHttpImpl])
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[ServiceLocatorRegistrationTask]).asEagerSingleton()

    bind(classOf[ApiAccess]).toInstance(
      ApiAccess("PRIVATE", configuration.underlying.getStringList("api.access.white-list.applicationIds").asScala))

    bindConfigBoolean("citizen-details.enabled", "microservice.services.citizen-details.enabled")

    bindConfigInt("controllers.confidenceLevel")
    bind(classOf[String]).annotatedWith(named("auth")).toInstance(baseUrl("auth"))
    bind(classOf[String]).annotatedWith(named("citizen-details")).toInstance(baseUrl("citizen-details"))
    bind(classOf[String]).annotatedWith(named("entity-resolver")).toInstance(baseUrl("entity-resolver"))
    bind(classOf[String]).annotatedWith(named("preferences")).toInstance(baseUrl("preferences"))
  }

  @Provides
  @Named("appName")
  def appName: String = AppName(configuration).appName

  /**
    * Binds a configuration value using the `path` as the name for the binding.
    * Throws an exception if the configuration value does not exist or cannot be read as an Int.
    */
  private def bindConfigInt(path: String): Unit =
    bindConstant().annotatedWith(named(path))
      .to(configuration.underlying.getInt(path))

  private def bindConfigBoolean(name: String, path: String): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getBoolean(path))
}