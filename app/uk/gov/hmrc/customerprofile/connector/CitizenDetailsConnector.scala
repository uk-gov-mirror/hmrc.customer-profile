package uk.gov.hmrc.customerprofile.connector

import play.api.Logger
import uk.gov.hmrc.customerprofile.config.WSHttp
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.config.ServicesConfig


trait CitizenDetailsConnector {

  import play.api.http.Status.LOCKED
  import uk.gov.hmrc.customerprofile.domain.PersonDetails
  import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, NotFoundException, Upstream4xxResponse}

  import scala.concurrent.{ExecutionContext, Future}

  def citizenDetailsConnectorUrl: String

  def http: HttpGet

  def personDetails(nino: Nino)(implicit hc: HeaderCarrier, ec : ExecutionContext): Future[PersonDetails] = {
    http.GET[PersonDetails](s"$citizenDetailsConnectorUrl/citizen-details/$nino/designatory-details") recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == LOCKED =>
        Logger.info("Person details are hidden")
        throw e
      case e: NotFoundException =>
        Logger.info(s"No details found for nino '$nino'")
        throw e
    }
  }
}

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {

  override lazy val citizenDetailsConnectorUrl = baseUrl("citizen-details")

  override lazy val http = WSHttp

}