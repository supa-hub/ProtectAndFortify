import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.3"

lazy val root = (project in file("."))
  .settings(
    name := "ProtectAndFortify",
    scalaVersion := "3.6.3",

    libraryDependencies += "org.scala-lang" %% "toolkit" % "0.7.0",
    libraryDependencies += "org.scalafx" % "scalafx_3" % "24.0.2-R36",
    libraryDependencies += "org.apache.commons" % "commons-math3" % "3.6.1",
    libraryDependencies +="org.scalatest" %% "scalatest" % "3.2.19" % "test",

    libraryDependencies += "org.typelevel" %% "cats-core" % "2.13.0",
    libraryDependencies += "co.fs2" %% "fs2-core" % "3.12.2",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.6.3"
  )
