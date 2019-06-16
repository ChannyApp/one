name := "one"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8",
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.22",
  "com.typesafe.akka" %% "akka-actor" % "2.5.22",
  "com.github.cb372" %% "scalacache-caffeine" % "0.27.0",
  "org.jsoup" % "jsoup" % "1.12.1"
)

test in assembly := {}
assemblyJarName in assembly := s"server.jar"
