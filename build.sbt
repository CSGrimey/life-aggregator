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

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val settings =
  commonSettings 

lazy val root = project 
  .in(file("."))
  .settings(
    settings
  )
  .disablePlugins(AssemblyPlugin)
  .aggregate(common, emailer, googleCalendarImporter)

lazy val common = project
  .settings(
    name := "common",
    settings
  )
  .disablePlugins(AssemblyPlugin)

lazy val emailer = project
  .settings(
    name := "emailer",
    settings,
    assemblySettings,
    libraryDependencies ++= Seq(
      dependencies.awsLambda
    )
  )
  .dependsOn(
    common
  )

lazy val googleCalendarImporter = project
  .settings(
    name := "googleCalendarImporter",
    settings
  )
  .dependsOn(
    common
  )

lazy val dependencies =
  new {
    val awsLambda        = "com.amazonaws" % "aws-lambda-java-core" % "1.2.3"
  }