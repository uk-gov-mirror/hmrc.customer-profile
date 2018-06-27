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

package uk.gov.hmrc.customerprofile.services

import org.scalamock.matchers.MatcherBase
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait BaseServiceSpec extends UnitSpec with MockFactory{
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val appNameConfiguration: Configuration = mock[Configuration]
  val auditConnector: AuditConnector = mock[AuditConnector]

  val appName = "customer-profile"

  def mockAudit(transactionName: String, detail: Map[String, String] =  Map.empty) = {
    def dataEventWith(auditSource: String,
                      auditType: String,
                      tags: Map[String, String]): MatcherBase = {
      argThat((dataEvent: DataEvent) => {
        dataEvent.auditSource.equals(auditSource) &&
          dataEvent.auditType.equals(auditType) &&
          dataEvent.tags.equals(tags) &&
          dataEvent.detail.equals(detail)
      })
    }

    (appNameConfiguration.getString(_: String, _: Option[Set[String]])).expects(
      "appName", None).returns(Some(appName)).anyNumberOfTimes()

    (auditConnector.sendEvent(_:DataEvent)(_: HeaderCarrier, _: ExecutionContext)).expects(
      dataEventWith(appName, auditType = "ServiceResponseSent", tags = Map("transactionName" -> transactionName)), *, *).returns(
      Future successful Success)
  }
}
