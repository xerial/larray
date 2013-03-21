import sbt._
import sbt.Keys._

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

  private val SCALA_VERSION = "2.10.0"

  lazy val root = Project(
    id = "larray",
    base = file("."),
    settings = Defaults.defaultSettings ++
      Seq(
        organization := "org.xerial",
        organizationName := "xerial.org",
        organizationHomepage := Some(new URL("http://xerial.org")),
        description := "Large array library for Java/Scala",
        publishMavenStyle := true,
        publishArtifact in Test := false,
        publishTo <<= version {
          (v) => Some(releaseResolver(v))
        },
        pomIncludeRepository := {
          _ => false
        },
        parallelExecution := true,
        parallelExecution in Test := false,
        scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature"),
        // custom settings here
        scalaVersion := SCALA_VERSION,
        crossPaths := false,
        libraryDependencies ++= Seq(
          // Add dependent jars here
          "org.xerial" % "xerial-core" % "3.1",
          "junit" % "junit" % "4.10" % "test",
          "com.novocode" % "junit-interface" % "0.10-M2" % "test",
          "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
          "org.scala-lang" % "scala-reflect" % SCALA_VERSION
        ),
        pomExtra := {
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
  )


}
