import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import sbt.Keys._
import sbt._

object Build
        extends sbt.Build
{
  private val SCALA_VERSION = "2.11.7"

  val buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "org.xerial.larray",
    organizationName := "xerial.org",
    organizationHomepage := Some(new URL("http://xerial.org")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := {
      _ => false
    },
    scalaVersion := SCALA_VERSION,
    logBuffered in Test := false,
    parallelExecution := true,
    parallelExecution in Test := false,
    javacOptions in Compile ++= Seq("-Xlint:unchecked"),
    javacOptions in(Compile, doc) <<= (baseDirectory, version) map { (bd, v) => Seq(
      "-locale", "en_US",
      "-sourcepath", bd.getAbsolutePath,
      "-doctitle", s"LArray ${v} API"
    )
    },
    scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature", "-target:jvm-1.6"),
    scalacOptions in(Compile, doc) <++= (baseDirectory, version) map { (bd, v) =>
      Seq("-sourcepath", bd.getAbsolutePath,
        "-doc-source-url", "https://github.com/xerial/larray/tree/develop/€{FILE_PATH}.scala",
        "-doc-title", "LArray API",
        "-doc-version", v,
        "-diagrams"
      )
    },
    testOptions in Test <+= (target in Test) map {
      t => Tests.Argument(TestFrameworks.ScalaTest, "junitxml(directory=\"%s\")".format(t / "test-reports"), "stdout")
    },
    crossPaths := true,
    pomExtra := {
      <url>https://github.com/xerial/larray</url>
              <licenses>
                <license>
                  <name>Apache 2</name>
                  <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
                </license>
              </licenses>
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
    }
  )

  lazy val root = Project(
    id = "larray-root",
    base = file("."),
    settings = buildSettings ++ Seq(
      publish := {},
      publishLocal := {}
    )
  ) aggregate(larrayScala, larrayBuffer, larrayMMap)

  object Dependency
  {
    val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.2.1"
    val junit = "junit" % "junit" % "4.10" % "test"
    val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.5"
  }

  import Dependency._

  private val scope = "test->test;compile->compile"

  lazy val larrayScala = Project(
    id = "larray",
    base = file("larray"),
    settings = buildSettings ++ SbtMultiJvm.multiJvmSettings ++
            Seq(
              description := "LArray: A Large off-heap arrays for Scala/Java",
              logBuffered in MultiJvm := false,
              crossScalaVersions := Seq("2.12.0-M3", "2.11.7", "2.10.6"),
              compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
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
                "org.xerial" %% "xerial-core" % "3.5.0",
                snappy % "test",
                junit,
                "org.iq80.snappy" % "snappy" % "0.3" % "test",
                "com.novocode" % "junit-interface" % "0.11" % "test",
                "org.scalatest" %% "scalatest" % "[2.2.5-M3,2.3)" % "test",
                "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
                "com.typesafe.akka" %% "akka-testkit" % "2.3.14" % "test",
                "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.3.14" % "test"
              )
            )
  ) dependsOn(larrayBuffer % scope, larrayMMap) configs (MultiJvm)

  lazy val larrayBuffer = Project(
    id = "larray-buffer",
    base = file("larray-buffer"),
    settings = buildSettings ++ Seq(
      description := "LArray off-heap buffer library",
      crossScalaVersions := Seq(SCALA_VERSION),
      crossPaths := false,
      autoScalaLibrary := false,
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "[2.2.5-M3,2.3)" % "test",
        "org.xerial.java" % "xerial-core" % "2.1",
        "org.xerial" %% "xerial-core" % "3.5.0" % "test"
        //        slf4j,
        //        slf4jSimple % "test"
      )
    )
  )

  lazy val larrayMMap = Project(
    id = "larray-mmap",
    base = file("larray-mmap"),
    settings = buildSettings ++
            Seq(
              description := "LArray mmap implementation",
              crossScalaVersions := Seq(SCALA_VERSION),
              crossPaths := false,
              autoScalaLibrary := false,
              libraryDependencies ++= Seq(
                snappy % "test",
                junit
              )
            )
  ) dependsOn (larrayBuffer % scope)
}
