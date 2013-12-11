import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{MultiJvm}

object Build extends sbt.Build {

  def releaseResolver(v: String): Resolver = {
    val profile = System.getProperty("profile", "default")
    profile match {
      case "default" => {
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          "snapshots" at nexus + "content/repositories/snapshots"
        else
          "releases" at nexus + "service/local/staging/deploy/maven2"
      }
      case p => {
        sys.error("unknown profile:%s".format(p))
      }
    }
  }

  private val SCALA_VERSION = "2.10.3"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.xerial",
    organizationName := "xerial.org",
    organizationHomepage := Some(new URL("http://xerial.org")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo <<= version {
      (v) => Some(releaseResolver(v))
    },
    pomIncludeRepository := {
      _ => false
    },
    scalaVersion := SCALA_VERSION,
    logBuffered in Test := false,
    parallelExecution := true,
    parallelExecution in Test := false,
    javacOptions in Compile ++= Seq("-Xlint:unchecked"),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature", "-target:jvm-1.6"),
    scalacOptions in (Compile, doc) <++= (baseDirectory, version) map { (bd, v) =>
      Seq("-sourcepath", bd.getAbsolutePath,
        "-doc-source-url", "https://github.com/xerial/larray/tree/develop/€{FILE_PATH}.scala",
        "-doc-title", "LArray API",
        "-doc-version", v,
        "-diagrams"
      )
    },
    testOptions in Test <+= (target in Test) map {
      t => Tests.Argument(TestFrameworks.ScalaTest, "junitxml(directory=\"%s\")".format(t /"test-reports" ), "stdout")
    },
    crossPaths := false,
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
  ) aggregate(larrayScala, larrayBuffer)


  object Dependency {

    val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.0" % "test"
    val junit  = "junit" % "junit" % "4.10" % "test"
    val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.5"
  }

  import Dependency._


  private val scope = "test->test;compile->compile"

  lazy val larrayScala = Project(
    id = "larray",
    base = file("larray-scala"),
    settings = buildSettings ++ SbtMultiJvm.multiJvmSettings ++
      Seq(
        description := "LArray: A Large off-heap arrays for Scala/Java",
        logBuffered in MultiJvm := false,
        compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
        executeTests in Test := {
          val testResults : Tests.Output = (executeTests in Test).value
          val multiJvmTestResults : Tests.Output = (executeTests in MultiJvm).value
          val results = testResults.events ++ multiJvmTestResults.events
          Tests.Output(
            Tests.overall(Seq(testResults.overall, multiJvmTestResults.overall)),
            results,
            testResults.summaries ++ multiJvmTestResults.summaries)
        },
        libraryDependencies ++= Seq(
          // Add dependent jars here
          "org.xerial" % "xerial-core" % "3.2.2",
          junit,
          "com.novocode" % "junit-interface" % "0.10-M2" % "test",
          "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
          "org.scalacheck" % "scalacheck_2.10" % "1.10.0" % "test",
          "com.typesafe.akka" %% "akka-testkit" % "2.2-M2" % "test",
          "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.2-M2" % "test"
        )
      )
  ) dependsOn(larrayBuffer % scope) configs(MultiJvm)

  lazy val larrayBuffer = Project(
    id = "larray-buffer",
    base = file("larray-buffer"),
    settings = buildSettings ++ Seq(
      description := "LArray off-heap buffer library",
      autoScalaLibrary := false,
      libraryDependencies ++= Seq(
        "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
        "org.xerial" % "xerial-core" % "3.2.2" % "test",
        slf4j,
        slf4jSimple % "test"
      )
    )
  )

  lazy val larrayMMap = Project(
    id = "larray-mmap",
    base = file("larray-mmap"),
    settings = buildSettings ++
      Seq(
        description := "LArray mmap implementation",
        libraryDependencies ++= Seq(snappy, junit)
      )
  ) dependsOn(larrayScala, larrayBuffer % scope)
}
