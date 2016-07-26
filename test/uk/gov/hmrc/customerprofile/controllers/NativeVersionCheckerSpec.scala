/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.test.Helpers._
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class NativeVersionCheckerSpec extends UnitSpec with WithFakeApplication with ScalaFutures with StubApplicationConfiguration {
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

  "nativeVersionChecker live controller " should {

    "return upgrade as false when the version does not require updating" in new SuccessNativeVersionChecker {
      val result = await(controller.validateAppVersion(None)(jsonDeviceVersionRequest))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe upgradeFalse
    }

    "return upgrade as true when the version requires updating" in new SuccessNativeVersionChecker {
      override lazy val upgrade = true

      val result = await(controller.validateAppVersion(None)(jsonDeviceVersionRequest))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe upgradeTrue
    }
  }

  "nativeVersionChecker Sandbox controller " should {

    "return upgrade as false when the version does not require updating" in new SuccessNativeVersionChecker {
      val result = await(controller.validateAppVersion(None)(jsonDeviceVersionRequest))

      status(result) shouldBe 200
      contentAsJson(result) shouldBe upgradeFalse
    }
  }
}

