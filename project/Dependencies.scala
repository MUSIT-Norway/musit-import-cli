import sbt._

object Dependencies {
  val scala = "2.11.8"

  val csvReader = "com.github.tototoshi" %% "scala-csv" % "1.3.4"
  val scalaTest = "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test"

  object Play {
    val version = "2.5.12"
    val playWs = "com.typesafe.play" %% "play-ws" % version
    val playJson = "com.typesafe.play" %% "play-json" % version
  }

  val rootDependencies: Seq[ModuleID] = Seq(
    csvReader,
    Play.playWs,
    Play.playJson,
    scalaTest
  )

}
