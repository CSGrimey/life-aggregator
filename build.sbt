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
    dependencies.circeCore,
    dependencies.circeGeneric,
    dependencies.circeParser,
    dependencies.logback,
    dependencies.log4cats,
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
    assemblySettings,
    libraryDependencies ++= Seq(
      dependencies.scalaTags
    )
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
      dependencies.googleApi,
      dependencies.http4sEmberClient,
      dependencies.http4sDsl,
      dependencies.http4sCirce
    )
  )
  .dependsOn(common)

lazy val dependencies =
  new {
    val awsLambda        = "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
    val logback = "ch.qos.logback" % "logback-classic" % "1.5.6"
    val log4cats = "org.typelevel" %% "log4cats-slf4j" % "2.7.0"
    val googleAuth = "com.google.auth" % "google-auth-library-oauth2-http" % "1.23.0"
    val googleApi = "com.google.apis" % "google-api-services-calendar" % "v3-rev20240419-2.0.0"
    val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"
    val weaver = "com.disneystreaming" %% "weaver-cats" % "0.8.4" % Test
    val http4sEmberClient = "org.http4s" %% "http4s-ember-client" % "0.23.27"
    val http4sDsl = "org.http4s" %% "http4s-dsl" % "0.23.27"
    val http4sCirce = "org.http4s" %% "http4s-circe" % "0.23.27"
    val circeCore = "io.circe" %% "circe-core" % "0.15.0-M1"
    val circeGeneric = "io.circe" %% "circe-generic" % "0.15.0-M1"
    val circeParser = "io.circe" %% "circe-parser" % "0.15.0-M1"
    val scalaTags = "com.lihaoyi" %% "scalatags" % "0.13.1"
  }