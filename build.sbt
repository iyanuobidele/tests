name := "circeTest"

version := "1.0"

scalaVersion := "2.11.8"

lazy val circeV = "0.5.1"
lazy val akkaV = "2.4.8"
lazy val sprayV = "1.3.2"

libraryDependencies := List(
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-routing-shapeless2" % sprayV,
  "io.spray" %% "spray-http" % sprayV,
  "io.spray" %% "spray-testkit" % sprayV % Test,
  "io.spray" %% "spray-json" % sprayV,
  "io.spray" %% "spray-client" % sprayV,
  "io.circe" %% "circe-core" % circeV,
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  "io.circe" %% "circe-spray" % circeV,
  "org.json4s" %% "json4s-jackson" % "3.5.0",
  "com.squareup.okhttp3" % "okhttp" % "3.5.0"
)
    