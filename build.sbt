import Dependencies._

inThisBuild(
  List(
    organization := "com.stackstate",
    scalaVersion := "2.12.13",
    crossScalaVersions := Seq(scalaVersion.value, "2.13.5"),
    version := "0.7.0",
    libraryDependencies ++= {
      // Silencer
      val silencerVersion = "1.7.3"

      Seq(
        compilerPlugin(("com.github.ghik" %% "silencer-plugin" % silencerVersion).cross(CrossVersion.full)),
        ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided).cross(CrossVersion.full)
      )
    }
  )
)

lazy val root = (project in file(".")).settings(
  name := "akka-http-pac4j",
  libraryDependencies ++= Seq(akkaHttp, akkaStream, pac4j, scalaTestCore % Test, scalacheck % Test, akkaHttpTestKit % Test, akkaStreamTestKit % Test),
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-explaintypes",
    "-unchecked",
    "-deprecation",
    "-feature",
    //"-language:higherKinds",
    "-Xlint",
    "-g:vars"
  ),
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "2.13") {
      Seq("-Werror", "-Wnumeric-widen", "-Wdead-code", "-Wvalue-discard", "-Wunused", "-Wmacros:after", "-Woctal-literal", "-Wextra-implicit")

    } else {
      Seq(
        "-Xfatal-warnings",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard",
        "-Ywarn-infer-any",
        "-Ywarn-unused",
        "-Ywarn-unused-import",
        "-Ywarn-macros:after"
      )
    }
  },
  scalacOptions in Test ~= {
    _.filterNot(_ == "-Werror")
  },
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "2.13") {
      Seq("-P:silencer:globalFilters=scala.jdk.CollectionConverters")
    } else {
      Seq.empty
    }
  },
  scalacOptions in Test += "-P:silencer:globalFilters=discarded\\ non-Unit",
  scalacOptions in (Compile, console) ~= {
    _.filterNot { opt =>
      opt.startsWith("-P") || opt.startsWith("-X") || opt.startsWith("-W")
    }
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot { opt =>
      opt.startsWith("-P") || opt.startsWith("-X") || opt.startsWith("-W")
    }
  }
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ =>
  false
}

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("https://github.com/StackVista/akka-http-pac4j"))

scmInfo := Some(ScmInfo(url("https://github.com/StackVista/akka-http-pac4j"), "scm:git@github.com:StackVista/akka-http-pac4j.git"))

developers := List(
  Developer(id = "hierynomus", name = "Jeroen van Erp", email = "jeroen@hierynomus.com", url = url("https://github.com/hierynomus")),
  Developer(id = "lmreis89", name = "Luis Reis", email = "lreis@stackstate.com", url = url("https://github.com/lmreis89")),
  Developer(id = "craffit", name = "Bram Schuur", email = "bschuur@stackstate.com", url = url("https://github.com/craffit")),
  Developer(id = "aacevedo", name = "Alejandro Acevedo", email = "aacevedoosorio@gmail.com", url = url("https://github.com/aacevedoosorio"))
)
