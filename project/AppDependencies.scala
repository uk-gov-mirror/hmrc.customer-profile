import sbt._

object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val microserviceBootstrapVersion = "6.13.0"
  private val authClientVersion = "2.4.0"
  private val domainVersion = "5.0.0"
  private val playHmrcApiVersion = "2.1.0"
  private val reactiveCircuitBreakerVersion = "3.2.0"
  private val emailaddressVersion = "2.2.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "auth-client" % authClientVersion,
    "uk.gov.hmrc" %% "play-hmrc-api" % playHmrcApiVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "reactive-circuit-breaker" % reactiveCircuitBreakerVersion,
    "uk.gov.hmrc" %% "emailaddress" % emailaddressVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  private val hmrcTestVersion = "3.0.0"
  private val mockitoVersion = "2.11.0"
  private val scalatestplusPlayVersion = "2.0.1"
  private val wiremockVersion = "2.3.1"

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope,
        "org.mockito" % "mockito-core" % mockitoVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestplusPlayVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
