import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer
import com.typesafe.sbt.SbtPgp.autoImportImpl.{PgpKeys, useGpg, usePgpKeyHex}
import play.sbt.PlayImport.{ehcache, filters, guice, ws}
import sbt.Keys._
import sbt.{url, _}
import sbtrelease.ReleasePlugin.autoImport._

object Dependencies {

  def subProject(id: String) = Project(id, file(id))
    .settings(publishSettings: _*)

  // release and publish settings
  val username = "asura-pro"
  val repo = "pea"

  import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

  val releaseSettings = Seq(
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
      releaseStepCommand("sonatypeReleaseAll")
      //pushChanges
    )
  )
  val publishSettings = Seq(
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

  /// Deps

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.6.10"
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaActor.revision
  val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.3"

  val commonDependencies = Seq(akkaActor, jackson)

  val commonPlayDeps = Seq(
    guice,
    ehcache,
    ws,
    filters
  )

  val playPac4jVersion = "10.0.2"
  val pac4jVersion = "4.1.0"
  val appPlayDeps = Seq(
    "org.pac4j" %% "play-pac4j" % playPac4jVersion,
    "org.pac4j" % "pac4j-http" % pac4jVersion,
    "org.pac4j" % "pac4j-ldap" % pac4jVersion,
    "org.pac4j" % "pac4j-jwt" % pac4jVersion,
    "com.typesafe.play" %% "play-json" % "2.7.4"
  ) ++ commonPlayDeps
}
