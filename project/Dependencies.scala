import sbt._

object Dependencies {
  val scalacheckVersion = "1.14.3"
  val akkaHttpVersion = "10.2.7"
  val akkaStreamsVersion = "2.6.18"
  val pac4jVersion = "5.7.7"
  val scalaTestVersion = "3.2.2"

  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  lazy val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion
  lazy val pac4j = "org.pac4j" % "pac4j-core" % pac4jVersion
  lazy val scalaTestCore = "org.scalatest" %% "scalatest" % scalaTestVersion
  lazy val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamsVersion
}
