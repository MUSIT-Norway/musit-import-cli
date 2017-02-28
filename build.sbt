import CommonSettings._

val noPublish = Seq(
  publish := {},
  publishLocal := {}
)

lazy val root = (
  MusitRootProject("musit-import-cli")
    settings noPublish
    settings(libraryDependencies ++= Dependencies.rootDependencies)
)
