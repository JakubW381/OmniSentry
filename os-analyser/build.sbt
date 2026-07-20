ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "os-analyser",
    idePackagePrefix := Some("dev.jakubw.omnisentry"),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .enablePlugins(JavaAppPackaging)
libraryDependencies ++= Seq(
  "com.github.haifengl" %% "smile-scala" % "3.0.2",
  "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.13" % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "0.11.13",
  "io.grpc" % "grpc-netty-shaded" % "1.70.0",

  "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.64.0",
  "io.opentelemetry" % "opentelemetry-exporter-otlp" % "1.64.0",
  "io.opentelemetry" % "opentelemetry-api" % "1.64.0",
  "io.opentelemetry" % "opentelemetry-sdk" % "1.64.0",
  "io.opentelemetry.instrumentation" % "opentelemetry-grpc-1.6" % "2.29.0-alpha"
)