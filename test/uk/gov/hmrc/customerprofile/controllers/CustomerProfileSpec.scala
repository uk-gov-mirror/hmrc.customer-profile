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

package uk.gov.hmrc.customerprofile.controllers

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.customerprofile.domain._
import uk.gov.hmrc.play.test.UnitSpec

class TestCustomerProfileGetAccountSpec extends UnitSpec with ScalaFutures with StubApplicationConfiguration {


  "getAccount live controller " should {

    "return the accounts successfully" in new Success {
      val result = await(controller.getAccounts()(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(Accounts(Some(nino), None, false, false, "102030394AAA"))
    }

    "return the accounts successfully when journeyId is supplied" in new Success {
      val result = await(controller.getAccounts(Some(journeyId))(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(Accounts(Some(nino), None, false, false, "102030394AAA"))
    }

    "return 401 result with json status detailing no nino on authority" in new AuthWithoutNino {
      testNoNINO(controller.getAccounts()(emptyRequestWithHeader))
    }

    "return 200 result with json status detailing low CL on authority" in new AuthWithLowCL {
      val result = await(controller.getAccounts()(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(Accounts(Some(nino), None, true, false, "102030394AAA"))
    }

    "return 200 result with json status detailing weak cred strength on authority" in new AuthWithWeakCreds {
      val result = await(controller.getAccounts()(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(Accounts(Some(nino), None, false, true, "102030394AAA"))
    }

    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.getAccounts()(emptyRequest))

      status(result) shouldBe 406
    }
  }

  "getAccount sandbox controller " should {

    "return the accounts response from a resource" in new SandboxSuccess {
      val result = await(controller.getAccounts()(emptyRequestWithHeader))

      status(result) shouldBe 200

      val journeyIdRetrieve: String = (contentAsJson(result) \ "journeyId").as[String]
      contentAsJson(result) shouldBe Json.toJson(Accounts(Some(nino), None, false, false, journeyIdRetrieve))
    }

    "return the accounts response from a resource when the journey Id is supplied" in new SandboxSuccess {
      val result = await(controller.getAccounts(Some(journeyId))(emptyRequestWithHeader))

      status(result) shouldBe 200

      val journeyIdRetrieve: String = (contentAsJson(result) \ "journeyId").as[String]
      contentAsJson(result) shouldBe Json.toJson(Accounts(Some(nino), None, false, false, journeyIdRetrieve))
    }


    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.getAccounts()(emptyRequest))

      status(result) shouldBe 406
    }
  }
}

class TestCustomerProfileGetPersonalDetailsSpec extends UnitSpec with ScalaFutures with StubApplicationConfiguration {

  "getPersonalDetails live " should {

    "return the PersonalDetails successfully" in new Success {
      val result: Result = await(controller.getPersonalDetails(nino)(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(person)
    }

    "return 401 when the nino in the request does not match the authority nino" in new AccessCheck {
      val result = await(controller.getPersonalDetails(nino)(emptyRequestWithHeader))

      status(result) shouldBe 401
    }

    "return the PersonalDetails successfully with journeyId" in new Success {
      val result: Result = await(controller.getPersonalDetails(nino, Some(journeyId))(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(person)
    }

    "return unauthorized when authority record does not contain a NINO" in new AuthWithoutNino {
      testNoNINO(await(controller.getPersonalDetails(nino)(emptyRequestWithHeader)))
    }

    "return 401 result with json status detailing low CL on authority" in new AuthWithLowCL {
      testLowCL(await(controller.getPersonalDetails(nino)(emptyRequestWithHeader)))
    }

    "return 401 result with json status detailing weak credStrength on authority" in new AuthWithWeakCreds {
      testWeakCredStrength(await(controller.getPersonalDetails(nino)(emptyRequestWithHeader)))
    }

    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.getPersonalDetails(nino)(emptyRequest))

      status(result) shouldBe 406
    }
  }

  "getPersonalDetails sandbox " should {

    "return the PersonalDetails response from a resource" in new SandboxSuccess {
      val result = await(controller.getPersonalDetails(nino)(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(person)
    }

    "return the PersonalDetails response from a resource with journeyId" in new SandboxSuccess {
      val result = await(controller.getPersonalDetails(nino,Some(journeyId))(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(person)
    }


    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.getPersonalDetails(nino)(emptyRequest))

      status(result) shouldBe 406
    }
  }
}

class TestCustomerProfilePreferencesSpec extends UnitSpec with ScalaFutures with StubApplicationConfiguration {

  "preferences live " should {

    "return the preferences successfully" in new Success {
      val result: Result = await(controller.getPreferences()(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.parse("""{"digital":true,"email":{"email":"someone@something.com","status":"pending"}}""")
    }

    "return the preference as off" in new Success {
      override lazy val defaultPreference = Some(Preference(false, None))

      val result: Result = await(controller.getPreferences()(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.parse("""{"digital":false}""")
    }

    "return 404 if no preference found for the user" in new PreferenceNotFound {
      val result: Result = await(controller.getPreferences()(emptyRequestWithHeader))

      status(result) shouldBe 404
    }

    "return unauthorized when authority record does not contain a NINO" in new AuthWithoutNino {
      testNoNINO(await(controller.getPreferences()(emptyRequestWithHeader)))
    }

    "return 401 result with json status detailing low CL on authority" in new AuthWithLowCL {
      testLowCL(await(controller.getPreferences()(emptyRequestWithHeader)))
    }

    "return 401 result with json status detailing weak credStrength on authority" in new AuthWithWeakCreds {
      testWeakCredStrength(await(controller.getPreferences()(emptyRequestWithHeader)))
    }

    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.getPreferences()(emptyRequest))

      status(result) shouldBe 406
    }
  }

  "preferences sandbox " should {

    "return the Preferences response from a resource" in new SandboxSuccess {
      val result = await(controller.getPreferences()(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe preferenceSuccess
    }

    "return the Preferences response from a resource with journeyId" in new SandboxSuccess {
      val result = await(controller.getPreferences(Some(journeyId))(emptyRequestWithHeader))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe preferenceSuccess
    }


    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.getPreferences()(emptyRequest))

      status(result) shouldBe 406
    }
  }
}

class TestCustomerProfilePaperlessSettingsSpec extends UnitSpec with ScalaFutures with StubApplicationConfiguration {

  "paperlessSettings live" should {

    "update paperless settings and 200 response code" in new Success {
      val result = await(controller.paperlessSettingsOptIn()(paperlessRequest))

      status(result) shouldBe 200
    }

    "update paperless settings and 200 response code with JourneyId" in new Success {
      val result = await(controller.paperlessSettingsOptIn(Some(journeyId))(paperlessRequest))

      status(result) shouldBe 200
    }

    "update paperless settings and 201 response code" in new SandboxPaperlessCreated {
      val result = await(controller.paperlessSettingsOptIn()(paperlessRequest))

      status(result) shouldBe 201
    }

    "return unauthorized when authority record does not contain a NINO" in new AuthWithoutNino {
      testNoNINO(await(controller.paperlessSettingsOptIn()(paperlessRequest)))
    }

    "return 401 result with json status detailing low CL on authority" in new AuthWithLowCL {
      testLowCL(await(controller.paperlessSettingsOptIn()(paperlessRequest)))
    }

    "return 401 result with json status detailing weak credStrength on authority" in new AuthWithWeakCreds {
      testWeakCredStrength(await(controller.paperlessSettingsOptIn()(paperlessRequest)))
    }

    "fail to update paperless settings and 500 response code" in new SandboxPaperlessFailed {
      val result = await(controller.paperlessSettingsOptIn()(paperlessRequest))

      status(result) shouldBe 500
    }

    "return status code 406 when the headers are invalid" in new Success {
      val result = await(controller.paperlessSettingsOptIn()(paperlessRequestNoAccept))

      status(result) shouldBe 406
    }
  }

  "paperlessSettings sandbox " should {

    "update paperless settings and 200 response code" in new SandboxSuccess {
      val result = await(controller.paperlessSettingsOptIn()(paperlessRequest))

      status(result) shouldBe 200
    }

    "update paperless settings and 200 response code with journeyId" in new SandboxSuccess {
      val result = await(controller.paperlessSettingsOptIn(Some(journeyId))(paperlessRequest))

      status(result) shouldBe 200
    }

    "return status code 406 when the headers are invalid" in new SandboxSuccess {
      val result = await(controller.paperlessSettingsOptIn()(paperlessRequestNoAccept))

      status(result) shouldBe 406
    }
  }

}
