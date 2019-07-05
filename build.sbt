name := "one"

version := "0.1"

scalaVersion := "2.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8",
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.6.0-M3",
  "com.typesafe.akka" %% "akka-actor" % "2.6.0-M3",
  "com.github.ben-manes.caffeine" % "caffeine" % "2.7.0",
  "org.jsoup" % "jsoup" % "1.12.1"
)

test in assembly := {}
assemblyJarName in assembly := s"server.jar"
