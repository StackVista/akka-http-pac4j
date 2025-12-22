import sbt._

object Dependencies {
  val scalacheckVersion = "1.14.3"
  val pekkoHttpVersion = "1.0.1"
  val pekkoStreamsVersion = "1.0.3"
  val pac4jVersion = "5.7.7"
  val scalaTestVersion = "3.2.2"

  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
  lazy val pekkoHttp = "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion
  lazy val pekkoHttpTestKit = "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion
  lazy val pekkoStream = "org.apache.pekko" %% "pekko-stream" % pekkoStreamsVersion
  lazy val pac4j = "org.pac4j" % "pac4j-core" % pac4jVersion
  lazy val scalaTestCore = "org.scalatest" %% "scalatest" % scalaTestVersion
  lazy val pekkoStreamTestKit = "org.apache.pekko" %% "pekko-stream-testkit" % pekkoStreamsVersion
  lazy val scalaCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.13.0"
}
