ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "untitled1",
    idePackagePrefix := Some("org.deplague"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.1",
      "dev.zio" %% "zio-http" % "3.0.0-RC9",
      "dev.zio" %% "zio-json" % "0.6.2",
      "dev.zio" %% "zio-test" % "2.1.1" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.1" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
