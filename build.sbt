import xerial.sbt.Sonatype._

Global / onChangedBuildSource := ReloadOnSourceChanges

sonatypeProfileName := "org.xerial"

val CROSS_SCALA_VERSIONS = Seq("2.12.17", "2.13.10", "3.2.1")
ThisBuild / scalaVersion := "3.2.1"

val buildSettings = Seq(
  organization             := "org.xerial.larray",
  organizationName         := "xerial.org",
  publishMavenStyle        := true,
  Test / publishArtifact   := false,
  crossScalaVersions       := CROSS_SCALA_VERSIONS,
  Test / logBuffered       := false,
  parallelExecution        := true,
  Test / parallelExecution := false,
  Compile / javacOptions ++= Seq("-Xlint:unchecked"),
  Compile / doc / javacOptions := Seq(
    "-locale",
    "en_US",
    "-sourcepath",
    baseDirectory.value.getAbsolutePath,
    "-doctitle",
    s"LArray ${version.value} API"
  ),
  scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature"),
  Compile / doc / scalacOptions ++= Seq(
    "-sourcepath",
    baseDirectory.value.getAbsolutePath,
    "-doc-source-url",
    "https://github.com/xerial/larray/tree/develop/€{FILE_PATH}.scala",
    "-doc-title",
    "LArray API",
    "-doc-version",
    version.value,
    "-diagrams"
  ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, major)) if major <= 12 =>
        Seq()
      case _ =>
        Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4")
    }
  } ++
    Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.8.1",
      "org.wvlet.airframe"     %% "airspec"                 % "22.11.4" % "test"
    ),
  testFrameworks += new TestFramework("wvlet.airspec.Framework"),
  crossPaths := true,
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  sonatypeProjectHosting := Some(GitHubHosting("xerial", "larray", "leo@xerial.org")),
  publishTo              := sonatypePublishToBundle.value
)

lazy val root = project
  .in(file("."))
  .settings(buildSettings)
  .settings(
    name            := "larray-root",
    publish         := {},
    publishLocal    := {},
    publishArtifact := false
  )
  .aggregate(larrayScala, larrayBuffer, larrayMMap)

val snappy      = "org.xerial.snappy" % "snappy-java"  % "1.1.4"
val junit       = "junit"             % "junit"        % "4.11" % "test"
val slf4j       = "org.slf4j"         % "slf4j-api"    % "1.7.25"
val slf4jSimple = "org.slf4j"         % "slf4j-simple" % "1.7.25"

val scope = "test->test;compile->compile"

lazy val larrayScala =
  project
    .in(file("larray"))
    .settings(buildSettings)
    .enablePlugins(MultiJvmPlugin)
    .configs(MultiJvm)
    .settings(
      name                   := "larray",
      description            := "LArray: A Large off-heap arrays for Scala/Java",
      MultiJvm / logBuffered := false,
      MultiJvm / jvmOptions ++= Seq("-Xmx128M"),
      MultiJvm / compile := { (MultiJvm / compile) triggeredBy (Test / compile) }.value,
      Test / executeTests := {
        val testResults: Tests.Output         = (Test / executeTests).value
        val multiJvmTestResults: Tests.Output = (MultiJvm / executeTests).value
        val results                           = testResults.events ++ multiJvmTestResults.events
        Tests.Output(
          Tests.overall(Seq(testResults.overall, multiJvmTestResults.overall)),
          results,
          testResults.summaries ++ multiJvmTestResults.summaries
        )
      },
      libraryDependencies ++= Seq(
        // Add dependent jars here
        "org.wvlet.airframe" %% "airframe-log" % "22.11.4",
        snappy                % "test",
        junit,
        "org.iq80.snappy"    % "snappy"                  % "0.3"    % "test",
        "com.github.sbt"     % "junit-interface"         % "0.13.3" % "test",
        "org.scalacheck"    %% "scalacheck"              % "1.15.4" % "test",
        "com.typesafe.akka" %% "akka-testkit"            % "2.7.0"  % "test",
        "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.7.0"  % "test"
      )
    ).dependsOn(larrayBuffer % scope, larrayMMap)

lazy val larrayBuffer = project
  .in(file("larray-buffer"))
  .settings(buildSettings)
  .settings(
    name             := "larray-buffer",
    description      := "LArray off-heap buffer library",
    crossPaths       := false,
    autoScalaLibrary := false
  )

lazy val larrayMMap = project
  .in(file("larray-mmap"))
  .settings(buildSettings)
  .settings(
    description      := "LArray mmap implementation",
    crossPaths       := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      snappy % "test",
      junit
    )
  ).dependsOn(larrayBuffer % scope)
