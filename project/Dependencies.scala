import sbt._

object DependencyConfig {
  val scalacheckVersion: String = "1.14.1"
  val akkaHttpVersion: String = "10.1.12"
  val akkaStreamsVersion: String = "2.5.31"
  val pac4jVersion: String = "3.6.1"
  val scalaTestVersion: String = "3.1.2"
}

object Dependencies {
  import DependencyConfig._

  def dependencies = Seq(
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamsVersion % Test,
    "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion,
    "org.pac4j" % "pac4j-core" % pac4jVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  )
}
