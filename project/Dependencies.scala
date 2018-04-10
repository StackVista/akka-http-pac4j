import sbt._

object Dependencies {
  lazy val specs2Core = "org.specs2" %% "specs2-core" % specs2Version
  lazy val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Version
  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion
  lazy val akkaStreams = "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion
  lazy val pac4j = "org.pac4j" % "pac4j-core" % pac4jVersion

  val specs2Version = "4.0.4-3593406-20180327185327"
  val scalacheckVersion = "1.13.5"
  val akkaHttpVersion = "10.0.11"
  val akkaStreamsVersion = "2.5.8"
  val pac4jVersion = "3.0.0-RC1"
}
