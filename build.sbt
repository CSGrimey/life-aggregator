name := "life-aggregator"
ThisBuild / organization := "grimes.charles"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.4.1"
ThisBuild / scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

assemblyJarName in assembly := s"${name.value}.jar"
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _ *) => MergeStrategy.discard
  case _                              => MergeStrategy.first
}
lazy val assemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case "application.conf"            => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val root = project 
  .in(file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(common, emailer, googleCalendarImporter)

lazy val common = project
  .settings(
    name := "common"
  )
  .disablePlugins(AssemblyPlugin)

lazy val emailer = project
  .settings(
    name := "emailer",
    assemblySettings,
    libraryDependencies ++= Seq(
      dependencies.awsLambda
    )
  )
  .dependsOn(
    common
  )

lazy val googleCalendarImporter = project

lazy val dependencies =
  new {
    val awsLambda        = "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
  }