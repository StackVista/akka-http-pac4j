lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.kensu-oss",
      scalaVersion := "2.13.2",
      version      := "1.0.0-SNAPSHOT"
    )),
    name := "akka-http-pac4j",
    libraryDependencies ++= Dependencies.dependencies,

    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-unchecked",
      //"-deprecation",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      //"-Wdead-code",
      //"-Werror",
      //"-Wself-implicit", //false positives with 2.13.1
      //"-Wunused",
      "-Wnumeric-widen",
      "-Xlint"
    ),
    scalacOptions in (Compile, console) --= Seq(
      "-Wunused:imports",
      "-Werror"
    )
  )

publishArtifact in (Compile, packageSrc) := true

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
