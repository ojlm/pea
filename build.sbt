name := """pea"""
organization := "cc.akkaha"
version := "0.1.0"
scalaVersion := "2.12.8"

lazy val pea = Project("pea", file("."))
  .enablePlugins(PlayScala)
  .settings(publishSettings: _*)

val gatling = "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.1.2" exclude("io.gatling", "gatling-app")
val gatlingCompiler = "io.gatling" % "gatling-compiler" % "3.1.2"
val curator = "org.apache.curator" % "curator-recipes" % "2.12.0"
val asuraCommon = "cc.akkaha" %% "asura-common" % "0.5.0"
val asuraPlay = "cc.akkaha" %% "asura-play" % "0.5.0"

libraryDependencies ++= Seq(gatling, gatlingCompiler, curator, asuraCommon, asuraPlay)
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test

// release
val username = "asura-pro"
val repo = "pea"

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

lazy val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    //runClean,
    // runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommand("publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    //pushChanges
  )
)
lazy val publishSettings = Seq(
  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses += "MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  apiURL := Some(url(s"https://$username.github.io/$repo/latest/api/")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  developers := List(
    Developer(
      id = username,
      name = "zhengshaodong",
      email = "ngxcorpio@gmail.com",
      url = new URL(s"http://github.com/${username}")
    )
  ),
  useGpg := true,
  usePgpKeyHex("200BB242B4BE47DD"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  // Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

coverageEnabled := false
