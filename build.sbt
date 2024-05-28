name := "life-aggregator"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := "grimes.charles"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8", "-Wnonunit-statement")

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := s"${name.value}.jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "MANIFEST.MF", xs @ _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
)

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
  ) ++ Resolver.sonatypeOssRepos("snapshots") ++ Resolver.sonatypeOssRepos("releases"),
  libraryDependencies ++= Seq(
    dependencies.awsLambda,
    dependencies.log4j,
    dependencies.log4jToSlf4j,
    dependencies.logback,
    dependencies.catsEffect,
    dependencies.weaver
  ),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(common, emailBuilder, googleCalendarImporter)

lazy val common = project
  .in(file("common"))
  .settings(
    name := "common",
    commonSettings
  )
  .disablePlugins(AssemblyPlugin)

lazy val emailBuilder = project
  .in(file("emailBuilder"))
  .settings(
    name := "emailBuilder",
    commonSettings,
    assembly / mainClass := Some("grimes.charles.Main"),
    assembly / test := (Test / test).value,
    assemblySettings
  ).dependsOn(common)

lazy val googleCalendarImporter = project
  .in(file("googleCalendarImporter"))
  .settings(
    name := "googleCalendarImporter",
    commonSettings,
    assembly / mainClass := Some("grimes.charles.Main"),
    assembly / test := (Test / test).value,
    assemblySettings,
    libraryDependencies ++= Seq(
      dependencies.googleAuth,
      dependencies.googleApi
    )
  )
  .dependsOn(common)

lazy val dependencies =
  new {
    val awsLambda        = "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
    val log4j = "org.apache.logging.log4j" % "log4j-api" % "2.23.1"
    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.23.1"
    val logback = "ch.qos.logback" % "logback-classic" % "1.5.6"
    val googleAuth = "com.google.auth" % "google-auth-library-oauth2-http" % "1.23.0"
    val googleApi = "com.google.apis" % "google-api-services-calendar" % "v3-rev20240419-2.0.0"
    val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"
    val weaver = "com.disneystreaming" %% "weaver-cats" % "0.8.4" % Test
  }