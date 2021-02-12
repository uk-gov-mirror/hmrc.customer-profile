/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customerprofile.connector

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, NotFoundException, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject() (
  @Named("citizen-details") citizenDetailsConnectorUrl: String,
  http:                                                 CoreGet) {

  import play.api.http.Status.LOCKED
  import uk.gov.hmrc.customerprofile.domain.PersonDetails

  def personDetails(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[PersonDetails] =
    http.GET[PersonDetails](s"$citizenDetailsConnectorUrl/citizen-details/$nino/designatory-details") recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == LOCKED =>
        Logger.info("Person details are hidden")
        throw e
      case e: NotFoundException =>
        Logger.info(s"No details found for nino '$nino'")
        throw e
    }
}
