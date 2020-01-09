import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.kensu-oss",
      scalaVersion := "2.11.12",
      version      := "0.6.0-SNAPSHOT",
      artifactClassifier := Some(s"akka-http-$akkaHttpBaseVersion")
    )),
    name := "akka-http-pac4j",
    libraryDependencies ++= Dependencies.dependencies,

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

publishArtifact in (Compile, packageSrc) := true

crossScalaVersions := Seq("2.12.4", scalaVersion.value)

pgpReadOnly := false

publishMavenStyle := true

// publishTo := {
//   val nexus = "https://oss.sonatype.org/"
//   if (isSnapshot.value)
//     Some("snapshots" at nexus + "content/repositories/snapshots")
//   else
//     Some("releases"  at nexus + "service/local/staging/deploy/maven2")
// }

pomIncludeRepository := { _ => false }

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("https://github.com/StackVista/akka-http-pac4j"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/StackVista/akka-http-pac4j"),
    "scm:git@github.com:StackVista/akka-http-pac4j.git"
  )
)

developers := List(
  Developer(
    id    = "lmreis89",
    name  = "Luis Reis",
    email = "lreis@stackstate.com",
    url   = url("https://github.com/lmreis89")
  ),
  Developer(
    id = "craffit",
    name = "Bram Schuur",
    email = "bschuur@stackstate.com",
    url = url("https://github.com/craffit")
  ),
  Developer(
    id = "aacevedo",
    name = "Alejandro Acevedo",
    email = "aacevedoosorio@gmail.com",
    url = url("https://github.com/aacevedoosorio")
  )
)
