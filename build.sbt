name := "life-aggregator"
//maintainer := "charlesgrimes90@protonmail.com"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := "grimes.charles"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

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
    dependencies.logback
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings
  )
  .disablePlugins(AssemblyPlugin)
  .aggregate(common, emailBuilder, googleCalendarImporter)
  //.enablePlugins(SbtNativePackager)
  //.enablePlugins(AwsLambdaPlugin)

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
    //compile / mainClass := Some("grimes.charles.Main"),
    //compile / packageDoc / mappings := Seq(),
    //compile / packageDoc / topLevelDirectory := None,
//    s3Bucket := Some("charles-grimes-manual-test"),
//    s3KeyPrefix := "deployment",
//    region := Some("eu-west-2"),
//    roleArn := Some("arn:aws:iam::471112823184:role/lambda_execution_role"),
//    deployMethod := Some("DIRECT"),
//    lambdaRuntime := "java17",
//    lambdaHandlers := Seq(
//      "emailBuilder"                 -> "grimes.charles.Main"
//    )
  )
  //.enablePlugins(JavaAppPackaging)
  //.enablePlugins(UniversalPlugin)
  .dependsOn(
    common
  )

lazy val googleCalendarImporter = project
  .in(file("googleCalendarImporter"))
  .settings(
    name := "googleCalendarImporter",
    commonSettings,
    assembly / mainClass := Some("grimes.charles.Main"),
    assembly / test := (Test / test).value,
    assemblySettings
  )
  .dependsOn(
    common
  )

lazy val dependencies =
  new {
    val awsLambda        = "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
    val log4j = "org.apache.logging.log4j" % "log4j-api" % "2.23.1"
    val log4jToSlf4j = "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.23.1"
    val logback = "ch.qos.logback" % "logback-classic" % "1.5.6"
  }