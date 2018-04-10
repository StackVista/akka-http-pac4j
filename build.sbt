import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "stackstate",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "akka-http-pac4j",
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaStreams,
      pac4j,
      scalactic,
      scalaTestCore % Test,
      scalacheck % Test
    ),
      scalacOptions ++= Seq(
  "-deprecation",           
  "-encoding", "UTF-8",
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",       
  "-Xlint",
  "-Yno-adapted-args",       
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture",
      ),
    scalacOptions in (Compile, console) --= Seq(
      "-Ywarn-unused-import",
      "-Xfatal-warnings"
    )
  )

useGpg := true

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}