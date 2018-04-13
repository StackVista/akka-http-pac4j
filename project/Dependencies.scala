import sbt._

object Dependencies {
  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion
  lazy val akkaStreams = "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion
  lazy val pac4j = "org.pac4j" % "pac4j-core" % pac4jVersion
  lazy val scalactic = "org.scalactic" %% "scalactic" % scalaTestVersion
  lazy val scalaTestCore = "org.scalatest" %% "scalatest" % scalaTestVersion
  lazy val mockitoCore = "org.mockito" % "mockito-core" % mockitoVersion

  val scalacheckVersion = "1.13.5"
  val akkaHttpVersion = "10.0.11"
  val akkaStreamsVersion = "2.5.8"
  val pac4jVersion = "3.0.0-RC1"
  val scalaTestVersion = "3.0.5"
  val mockitoVersion = "1.10.19"

}
