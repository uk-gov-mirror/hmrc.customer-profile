import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName: String = "customer-profile"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(publishingSettings: _*)
  .settings(
    Seq(
      routesImport ++= Seq(
        "uk.gov.hmrc.domain._",
        "uk.gov.hmrc.customerprofile.binder.Binders._",
        "uk.gov.hmrc.customerprofile.domain.types._",
        "uk.gov.hmrc.customerprofile.domain.types.ModelTypes._"
      )
    )
  )
  .settings(
    majorVersion := 1,
    scalaVersion := "2.12.8",
    playDefaultPort := 8233,
    libraryDependencies ++= AppDependencies(),
    dependencyOverrides ++= AppDependencies.overrides(),
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers += Resolver.jcenterRepo,
    unmanagedResourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base =>
      Seq(base / "it-resources")
    ).value,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      //"-Ywarn-unused-import", - does not work well with fatal-warnings because of play-generated sources
      //"-Xfatal-warnings",
      "-Xlint"
    ),
    coverageMinimum := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;.*Routes.*;app.*;.*prod;.*definition;.*testOnlyDoNotUseInAppConf;.*com.kenshoo.*;.*javascript.*;.*BuildInfo;.*Reverse.*;.*Binders.*;"
  )

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
