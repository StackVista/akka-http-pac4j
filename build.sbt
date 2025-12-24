import Dependencies._

inThisBuild(List(organization := "com.stackstate", scalaVersion := "2.13.13"))

lazy val root = (project in file(".")).settings(
  name := "pekko-http-pac4j",
  libraryDependencies ++= Seq(
    pekkoHttp,
    pekkoStream,
    pac4j,
    scalaCompat,
    scalaTestCore % Test,
    scalacheck % Test,
    pekkoHttpTestKit % Test,
    pekkoStreamTestKit % Test
  ),
  scalacOptions ++= Seq("-encoding", "UTF-8", "-explaintypes", "-unchecked", "-deprecation", "-feature", "-Xlint", "-g:vars"),
  scalacOptions ++= Seq("-Werror", "-Wnumeric-widen", "-Wdead-code", "-Wvalue-discard", "-Wmacros:after", "-Woctal-literal", "-Wextra-implicit"),
  Test / scalacOptions := Seq("-Wconf:msg=unused value of type org.scalatest.Assertion:s"),
  Compile / console / scalacOptions ~= {
    _.filterNot { opt =>
      opt.startsWith("-P") || opt.startsWith("-X") || opt.startsWith("-W")
    }
  },
  Test / console / scalacOptions ~= {
    _.filterNot { opt =>
      opt.startsWith("-P") || opt.startsWith("-X") || opt.startsWith("-W")
    }
  }
)

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
