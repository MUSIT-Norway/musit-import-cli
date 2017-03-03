import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
import sbt._

import scalariform.formatter.preferences.{FormatXml, SpacesAroundMultiImports}

object CommonSettings {
  val projectSettings = Seq(
    organization := "no.uio.musit",
    scalaVersion := Dependencies.scala,
    // Print log statements as they happen instead of doing it out of band.
    logBuffered in Test := false,
    scalacOptions := Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xlint", // Enable recommended additional warnings.
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      "-Ywarn-numeric-widen", // Warn when numerics are widened.
      // For advanced language features
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Xmax-classfile-name", "100" // This will limit the classname generation to 100 characters.
    ),
    // Disable scaladoc
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Seq.empty
  )

  // scalastyle:off
  def MusitRootProject(projName: String): Project = BaseProject(projName, ".")
        .enablePlugins(JavaAppPackaging)

  def BaseProject(projName: String): Project = BaseProject(projName, projName)

  private def BaseProject(projName: String, path: String): Project =
    Project(projName, file(path))
        .settings(projectSettings: _*)
        .settings(SbtScalariform.scalariformSettingsWithIt ++ Seq(
          ScalariformKeys.preferences := ScalariformKeys.preferences.value
              .setPreference(FormatXml, false)
              .setPreference(SpacesAroundMultiImports, false)
        ))
        .settings(
          // Setting timezone for testing to UTC, because h2 doesn't support
          // timezones very well, and it will always default to UTC regardless.
          // For production environments we're using the timezone configured at
          // OS level for each running service.
          javaOptions in Test += "-Duser.timezone=UTC"
        )
        .settings(dependencyOverrides += Dependencies.scalaTest)
        .configs(IntegrationTest)
  // scalastyle:on
}