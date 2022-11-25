// sbt-scoverage upgraded to scala-xml 2.1.0, but other sbt-plugins and Scala compilier 2.12 uses scala-xml 1.x.x
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

addSbtPlugin("org.xerial.sbt"   % "sbt-pack"      % "0.17")
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"  % "3.9.15")
addSbtPlugin("com.github.sbt"   % "sbt-pgp"       % "2.2.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.4.0")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"  % "2.5.0")

scalacOptions ++= Seq("-deprecation", "-feature")
