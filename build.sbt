sonatypeProfileName := "org.xerial"

import ReleaseTransformations._

val SCALA_VERSION = "2.12.1"
scalaVersion := SCALA_VERSION

val buildSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "org.xerial.larray",
  organizationName := "xerial.org",
  organizationHomepage := Some(new URL("http://xerial.org")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  scalaVersion := SCALA_VERSION,
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
  )
)

lazy val root = Project(
  id = "larray-root",
  base = file("."),
  settings = buildSettings ++ Seq(
    publish := {},
    publishLocal := {}
  )
) aggregate(larrayScala, larrayBuffer, larrayMMap)

val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.2.4"
val junit = "junit" % "junit" % "4.10" % "test"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.5"

val scope = "test->test;compile->compile"

lazy val larrayScala = Project(
  id = "larray",
  base = file("larray"),
  settings = buildSettings ++ SbtMultiJvm.multiJvmSettings ++
    Seq(
      crossScalaVersions := Seq("2.12.1", "2.11.8"),
      description := "LArray: A Large off-heap arrays for Scala/Java",
      logBuffered in MultiJvm := false,
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
        "org.xerial" %% "xerial-core" % "3.6.0",
        snappy % "test",
        junit,
        "org.iq80.snappy" % "snappy" % "0.3" % "test",
        "com.novocode" % "junit-interface" % "0.11" % "test",
        "org.scalatest" %% "scalatest" % "3.0.1" % "test",
        "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
        "com.typesafe.akka" %% "akka-testkit" % "[2.3.14, 2.5)" % "test",
        "com.typesafe.akka" %% "akka-multi-node-testkit" % "[2.3.14, 2.5)" % "test"
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
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.xerial.java" % "xerial-core" % "2.1",
      "org.xerial" %% "xerial-core" % "3.6.0" % "test"
    )
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
