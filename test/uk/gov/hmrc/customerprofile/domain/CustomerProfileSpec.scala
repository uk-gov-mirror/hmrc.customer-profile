package uk.gov.hmrc.customerprofile.domain

import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

class CustomerProfileSpec extends UnitSpec with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  "create CustomerProfile" in {
      val nino = Some(Nino("CS100700A"))
      val saUtr = Some(SaUtr("1872796160"))
      def accounts : () => Future[Accounts] = () => Future.successful(Accounts(nino, saUtr))

      def pd(nino: Option[Nino]) : Future[PersonDetails] = {
        val person = Person(Some("Mike"), None, Some("Potter"), None, Some("Mr"), None, Some("M"), Some(DateTimeUtils.now), nino)
        Future.successful(PersonDetails("etag1234",person, None, None))
      }

      val customerProfile = CustomerProfile.create(accounts, pd).futureValue

      customerProfile.accounts.nino.get shouldBe nino.get
      customerProfile.accounts.saUtr.get shouldBe saUtr.get

      customerProfile.personalDetails.etag shouldBe "etag1234"
      customerProfile.personalDetails.person.firstName.get shouldBe "Mike"
  }
}
