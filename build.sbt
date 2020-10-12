import Dependencies._

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-language:postfixOps", "-language:higherKinds", "-language:implicitConversions")
ThisBuild / organization := "cc.akkaha"
ThisBuild / version := "0.8.0"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / maintainer := "ngxcorpio@gmail.com"

lazy val pea = Project("pea", file("."))
  .enablePlugins(PlayScala)
  .settings(publishSettings: _*)
  .dependsOn(
    peaCommon % "compile->compile;test->test",
    peaDubbo % "compile->compile;test->test",
    peaGrpc % "compile->compile;test->test",
  ).aggregate(peaCommon, peaDubbo, peaGrpc)

// pea-app dependencies
val gatlingVersion = "3.4.0"
val gatling = "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion exclude("io.gatling", "gatling-app")
val gatlingCompiler = "io.gatling" % "gatling-compiler" % gatlingVersion
val curator = "org.apache.curator" % "curator-recipes" % "2.12.0"
val oshiCore = "com.github.oshi" % "oshi-core" % "5.2.5"

libraryDependencies ++= Seq(akkaStream, gatling, gatlingCompiler, curator, oshiCore) ++ appPlayDeps

// pea-common
lazy val peaCommon = subProject("pea-common")
  .settings(libraryDependencies ++= commonDependencies)

// pea-dubbo
val dubbo = "org.apache.dubbo" % "dubbo" % "2.7.4.1"
lazy val peaDubbo = subProject("pea-dubbo")
  .settings(libraryDependencies ++= Seq(
    gatling, dubbo,
  ))

// pea-grpc
val grpcVersion = "1.22.2" // override 1.8, com.trueaccord.scalapb.compiler.Version.grpcJavaVersion
val grpcNetty = "io.grpc" % "grpc-netty" % grpcVersion exclude("io.netty", "netty-codec-http2") // be compatible with gatling(4.1.42.Final)
val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
lazy val peaGrpc = subProject("pea-grpc")
  .settings(libraryDependencies ++= Seq(
    gatling, grpcNetty, scalapbRuntime
  ))

// options: https://github.com/thesamet/sbt-protoc
PB.protoSources in Compile := Seq(
  baseDirectory.value / "test/protobuf"
)
PB.targets in Compile := Seq(
  scalapb.gen(grpc = true) -> baseDirectory.value / "test-generated"
)
unmanagedSourceDirectories in Compile += baseDirectory.value / "test-generated"
sourceGenerators in Compile -= (PB.generate in Compile).taskValue

coverageEnabled := false
