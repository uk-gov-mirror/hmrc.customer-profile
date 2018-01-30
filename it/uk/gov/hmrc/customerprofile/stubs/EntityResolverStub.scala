package uk.gov.hmrc.customerprofile.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object EntityResolverStub {

  private def entityDetailsByNino(nino: String, entityId: String) = s"""
                                       |{
                                       |  "_id":"$entityId",
                                       |  "sautr":"8040200778",
                                       |  "nino":"$nino"
                                       |}""".stripMargin

  private def urlEqualToEntityResolverPaye(nino: String) = {
    urlEqualTo(s"/entity-resolver/paye/${nino}")
  }

  def respondWithEntityDetailsByNino(nino: String, entityId: String) =
  stubFor(get(urlEqualToEntityResolverPaye(nino))
    .willReturn(aResponse()
    .withStatus(200)
    .withBody(entityDetailsByNino(nino, entityId))))


}
