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

package uk.gov.hmrc.customerprofile.config

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.api.config.ServiceLocatorConfig
import uk.gov.hmrc.api.connector.ServiceLocatorConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.ws._

trait Hooks extends HttpHooks with HttpAuditing {
  val hooks = Seq(AuditingHook)
}

class WSHttpImpl @Inject()(@Named("appName") val appName: String, val auditConnector: AuditConnector) extends HttpClient with WSGet
  with WSPut
  with WSPost
  with WSDelete
  with WSPatch
  with Hooks

class MicroserviceAudit @Inject()(@Named("appName") val applicationName: String,
                                  val auditConnector: AuditConnector) extends Audit(applicationName, auditConnector)

@Singleton
class ApiServiceLocatorConnector @Inject()(override val runModeConfiguration: Configuration, environment: Environment, wsHttp: WSHttpImpl)
  extends ServiceLocatorConnector with ServiceLocatorConfig with AppName {
  override val appUrl: String = runModeConfiguration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  override val serviceUrl: String = serviceLocatorUrl
  override val handlerOK: () => Unit = () => Logger.info("Service is registered on the service locator")
  override val handlerError: Throwable => Unit = e ⇒ Logger.error("Service could not register on the service locator", e)
  override val metadata: Option[Map[String, String]] = Some(Map("third-party-api" → "true"))
  override val http: CorePost = wsHttp

  override def appNameConfiguration: Configuration = runModeConfiguration

  override protected def mode: Mode = environment.mode
}