sonatypeProfileName := "org.xerial"

import ReleaseTransformations._

val SCALA_VERSION = "2.13.1"
val CROSS_SCALA_VERSIONS = Seq(SCALA_VERSION, "2.11.12", "2.12.10")
scalaVersion in ThisBuild := SCALA_VERSION

val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "org.xerial.larray",
  organizationName := "xerial.org",
  organizationHomepage := Some(new URL("http://xerial.org")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  scalaVersion := SCALA_VERSION,
  crossScalaVersions := CROSS_SCALA_VERSIONS,
  logBuffered in Test := false,
  parallelExecution := true,
  parallelExecution in Test := false,
  javacOptions in Compile ++= Seq("-Xlint:unchecked"),
  javacOptions in(Compile, doc) := Seq(
    "-locale", "en_US",
    "-sourcepath", baseDirectory.value.getAbsolutePath,
    "-doctitle", s"LArray ${version.value} API"
  ),
  scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature"),
  scalacOptions in(Compile, doc) ++= Seq("-sourcepath", baseDirectory.value.getAbsolutePath,
    "-doc-source-url", "https://github.com/xerial/larray/tree/develop/€{FILE_PATH}.scala",
    "-doc-title", "LArray API",
    "-doc-version", version.value,
    "-diagrams"
  ),
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", s"${target.value / "test-reports"}", "-o"),
  crossPaths := true,
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  homepage := Some(url("https://github.com/xerial/larray")),
  pomExtra := {
      <scm>
        <connection>scm:git:github.com/xerial/larray.git</connection>
        <developerConnection>scm:git:git@github.com:xerial/larray.git</developerConnection>
        <url>github.com/xerial/larray.git</url>
      </scm>
      <properties>
        <scala.version>
          {SCALA_VERSION}
        </scala.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      </properties>
      <developers>
        <developer>
          <id>leo</id>
          <name>Taro L. Saito</name>
          <url>http://xerial.org/leo</url>
        </developer>
      </developers>
  },
  releaseTagName := { (version in ThisBuild).value },
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
    pushChanges
  ),
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  )
)

lazy val root = Project(
  id = "larray-root",
  base = file("."),
  settings = buildSettings ++ Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )
) aggregate(larrayScala, larrayBuffer, larrayMMap)

val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.4"
val junit = "junit" % "junit" % "4.11" % "test"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.25"
val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.25"

val scope = "test->test;compile->compile"

lazy val larrayScala = Project(
  id = "larray",
  base = file("larray"),
  settings = buildSettings ++ SbtMultiJvm.multiJvmSettings ++
    Seq(
      description := "LArray: A Large off-heap arrays for Scala/Java",
      logBuffered in MultiJvm := false,
      jvmOptions in MultiJvm ++= Seq("-Xmx128M"),
      compile in MultiJvm := {(compile in MultiJvm) triggeredBy (compile in Test)}.value,
      executeTests in Test := {
        val testResults: Tests.Output = (executeTests in Test).value
        val multiJvmTestResults: Tests.Output = (executeTests in MultiJvm).value
        val results = testResults.events ++ multiJvmTestResults.events
        Tests.Output(
          Tests.overall(Seq(testResults.overall, multiJvmTestResults.overall)),
          results,
          testResults.summaries ++ multiJvmTestResults.summaries)
      },
      libraryDependencies ++= Seq(
        // Add dependent jars here
        "org.wvlet.airframe" %% "airframe-log" % "19.9.8",
        snappy % "test",
        junit,
        "org.iq80.snappy" % "snappy" % "0.3" % "test",
        "com.novocode" % "junit-interface" % "0.11" % "test",
        "org.scalatest" %% "scalatest" % "3.0.8" % "test",
        "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
      )
    )
) dependsOn(larrayBuffer % scope, larrayMMap) configs (MultiJvm)

lazy val larrayBuffer = Project(
  id = "larray-buffer",
  base = file("larray-buffer"),
  settings = buildSettings ++ Seq(
    description := "LArray off-heap buffer library",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= {
      val parallelCollections = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, major)) if major >= 13 =>
          Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0" % "test")
        case _ =>
          Seq()
      }
      Seq(
        "org.scalatest" %% "scalatest" % "3.0.8" % "test",
        "org.wvlet.airframe" %% "airframe-log" % "19.9.8"
      ) ++ parallelCollections
    }
  )
)

lazy val larrayMMap = Project(
  id = "larray-mmap",
  base = file("larray-mmap"),
  settings = buildSettings ++
    Seq(
      description := "LArray mmap implementation",
      crossPaths := false,
      autoScalaLibrary := false,
      libraryDependencies ++= Seq(
        snappy % "test",
        junit
      )
    )
) dependsOn (larrayBuffer % scope)
