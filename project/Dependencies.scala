import sbt._

case class DependencyConfig(
                             scalacheckVersion: String = "1.13.5",
                             akkaHttpVersion: String = "10.1.8",
                             akkaStreamsVersion: String = "2.5.19",
                             pac4jVersion: String = "3.6.1",
                             scalaTestVersion: String = "3.0.7"
                           )

object Dependencies {
  val akkaHttpBaseVersion = System.getProperty("akka-http.version", "10.1")
  val depsConfig = akkaHttpBaseVersion match {
    case "10.1" =>
      DependencyConfig()

    case "10.0" =>
      DependencyConfig(
        scalacheckVersion = "1.13.5",
        akkaHttpVersion = "10.0.11",
        akkaStreamsVersion = "2.5.8",
        pac4jVersion = "3.6.1",
        scalaTestVersion = "3.0.5"
      )

    case _ => throw new IllegalArgumentException("Unsupported akka-http version")
  }
  import depsConfig._

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
