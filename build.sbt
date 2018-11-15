name := "expense-recorder"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.5",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",
  "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "1.0-M1",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.pauldijou" %% "jwt-core" % "0.19.0"
)