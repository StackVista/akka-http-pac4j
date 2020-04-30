import sbt._

object Dependencies {
  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  lazy val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion
  lazy val pac4j = "org.pac4j" % "pac4j-core" % pac4jVersion
  lazy val scalaTestCore = "org.scalatest" %% "scalatest" % scalaTestVersion
  lazy val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamsVersion

  val scalacheckVersion = "1.14.3"
  val akkaHttpVersion = "10.1.9"
  val akkaStreamsVersion = "2.5.25"
  val pac4jVersion = "3.8.3"
  val scalaTestVersion = "3.1.1"
}
