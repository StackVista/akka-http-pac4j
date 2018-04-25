import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.stackvista",
      scalaVersion := "2.12.4",
      version      := "0.2.0-SNAPSHOT"
    )),
    name := "akka-http-pac4j",
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaStreams,
      pac4j,
      scalaTestCore % Test,
      scalacheck % Test,
      akkaHttpTestKit % Test
    ),
      scalacOptions ++= Seq(
  "-deprecation",           
  "-encoding", "UTF-8",
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  // "-Xfatal-warnings", // TODO: Should be reenabled when implementation is complete
  "-Xlint",
  "-Yno-adapted-args",
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