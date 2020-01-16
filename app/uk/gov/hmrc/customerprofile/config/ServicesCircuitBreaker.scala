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

package uk.gov.hmrc.customerprofile.config

import play.api.{Configuration, Environment}
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.config.RunMode

trait ServicesCircuitBreaker extends UsingCircuitBreaker {

  def configuration: Configuration
  def environment:   Environment
  protected val externalServiceName: String

  private lazy val runMode = new RunMode(configuration, environment.mode)

  // Annoyingly, the `config` method in `ServicesConfig` is protected, although it is never used within the class,
  // so I've had to duplicated the code for it here as there doesn't seem to be an obvious alternative to it
  protected lazy val rootServices = "microservice.services"
  protected lazy val services     = s"${runMode.env}.microservice.services"

  def config(serviceName: String): Configuration =
    configuration
      .getOptional[Configuration](s"$rootServices.$serviceName")
      .orElse(configuration.getOptional[Configuration](s"$services.$serviceName"))
      .getOrElse(throw new IllegalArgumentException(s"Configuration for service $serviceName not found"))

  override protected def circuitBreakerConfig = CircuitBreakerConfig(
    serviceName = externalServiceName,
    numberOfCallsToTriggerStateChange =
      config(externalServiceName).getOptional[Int]("circuitBreaker.numberOfCallsToTriggerStateChange"),
    unavailablePeriodDuration = config(externalServiceName)
        .getOptional[Int]("circuitBreaker.unavailablePeriodDurationInSeconds") map (_ * 1000),
    unstablePeriodDuration = config(externalServiceName)
        .getOptional[Int]("circuitBreaker.unstablePeriodDurationInSeconds") map (_ * 1000)
  )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case _: BadRequestException => false
    case _: NotFoundException   => false
    case _: Upstream4xxResponse => false
    case _: Upstream5xxResponse => true
    case _ => true
  }
}
