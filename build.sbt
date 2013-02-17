  seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

  mainClass in oneJar := Some("scalashot.Scalashot")

  name := "scalashot"

  organization := "scalashot"

  version := "0.1"

  scalaVersion := "2.9.2"

  libraryDependencies := Seq(
    "org.imgscalr" % "imgscalr-lib" % "4.2",
    "org.scala-lang" % "scala-swing" % "2.9.2",
    "commons-lang" % "commons-lang" % "2.6",
    "commons-codec" % "commons-codec" % "1.6",
    "commons-collections" % "commons-collections" % "3.2.1",
    "commons-logging" % "commons-logging" % "1.1.1",
    "org.apache.httpcomponents" % "httpclient" % "4.2.1",
    "org.apache.httpcomponents" % "httpmime" % "4.2.1",
    "org.apache.httpcomponents" % "httpcore" % "4.2.1",
    "com.googlecode.json-simple" % "json-simple" % "1.1.1"
   )