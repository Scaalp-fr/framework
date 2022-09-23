import Dependencies._
import LiftSbtHelpers._

organization in ThisBuild          := "net.liftweb"
version in ThisBuild               := "3.6.0-SNAPSHOT"
homepage in ThisBuild              := Some(url("https://www.liftweb.net"))
licenses in ThisBuild              += ("Apache License, Version 2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
startYear in ThisBuild             := Some(2006)
organizationName in ThisBuild      := "WorldWide Conferencing, LLC"

val scala211Version = "2.11.12"
// Bug in compiler for versions 2.12.13, 14 and 15. StringBuiledr.length()
// cannot be written with ().
val scala212Version = "2.12.12"
val scala213Version = "2.13.9"

val crossUpTo212 = Seq(scala212Version, scala211Version)
val crossUpTo213 = scala213Version +: crossUpTo212

scalaVersion       in ThisBuild    := scala213Version
crossScalaVersions in ThisBuild    := crossUpTo212 // default everyone to 2.12 for now

libraryDependencies in ThisBuild ++= Seq(specs2, specs2Matchers, specs2Mock, scalacheck, scalactic, scalatest)
libraryDependencies in ThisBuild ++= {
  scalaVersion.value.split("\\.") match {
    case Array("2", m, p) =>
      val minor = m.toInt
      val patch = p.toInt

      // See published versions at: https://repo1.maven.org/maven2/com/github/ghik/
      val silencerVersion = minor match {
          case 11 => Some("1.7.7")
          case 12 => if (patch <= 10) Some("1.6.0")
                     else if (patch <= 12) Some("1.7.1")
                     else None // Some("1.7.7"), nowarn is backported since 2.12.13
          case 13 => if (patch == 1) Some("1.6.0")
                     else None // Some("1.7.7"), nowarn is backported since 2.13.2
        }

      (
        // Allows @scala.annotation.nowarn
        silencerVersion match {
          case Some(version) =>
            List(
              compilerPlugin("com.github.ghik" % "silencer-plugin" % version cross CrossVersion.full),
              "com.github.ghik" % "silencer-lib" % version % Provided cross CrossVersion.full,
              )
          case None => Nil
        }
      ) ++ (
        // Allows import scala.jdk.CollectionConverters._
        if (minor < 13) List(scala_compat)
        else Nil
      )
    case _ => Nil
  }
}

// Settings for Sonatype compliance
pomIncludeRepository in ThisBuild := { _ => false }
publishTo in ThisBuild := {
  if (isSnapshot.value) {
    Some(Opts.resolver.sonatypeSnapshots)
  } else {
    Some(Opts.resolver.sonatypeStaging)
  }
}
scmInfo in ThisBuild   := Some(ScmInfo(url("https://github.com/lift/framework"), "scm:git:https://github.com/lift/framework.git"))
pomExtra in ThisBuild  := Developers.toXml

publish / skip := true
//credentials in ThisBuild += Credentials(BuildPaths.getGlobalSettingsDirectory(state.value, BuildPaths.getGlobalBase(state.value)) / ".credentials")

initialize := {
  printLogo(name.value, version.value, scalaVersion.value)
}

resolvers  in ThisBuild  ++= Seq(
  "snapshots"     at "https://oss.sonatype.org/content/repositories/snapshots",
  "releases"      at "https://oss.sonatype.org/content/repositories/releases"
)

lazy val liftProjects = core ++ web ++ persistence

lazy val framework =
  liftProject("lift-framework", file("."))
    .aggregate(liftProjects: _*)
    .enablePlugins(ScalaUnidocPlugin)

// Core Projects
// -------------
lazy val core: Seq[ProjectReference] =
//  Seq(common, actor, markdown, json, json_scalaz7, json_ext, util)
  Seq(common, actor, markdown, json, util)

lazy val common =
  coreProject("common")
    .settings(
      description := "Common Libraties and Utilities",
      libraryDependencies ++= Seq(slf4j_api, logback, slf4j_log4j12, scala_xml, scala_parser)
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val actor =
  coreProject("actor")
    .dependsOn(common)
    .settings(
      description := "Simple Actor",
      parallelExecution in Test := false
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val markdown =
  coreProject("markdown")
    .settings(
      description := "Markdown Parser",
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(scalatest, scalatest_junit, scala_xml, scala_parser)
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val json =
  coreProject("json")
    .settings(
      description := "JSON Library",
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(scalap(scalaVersion.value), paranamer,  scala_xml)
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val documentationHelpers =
  coreProject("documentation-helpers")
    .settings(description := "Documentation Helpers")
    .dependsOn(util)
    .settings(crossScalaVersions := crossUpTo213)

lazy val json_scalaz7 =
  coreProject("json-scalaz7")
    .dependsOn(json)
    .settings(
      description := "JSON Library based on Scalaz 7",
      libraryDependencies ++= Seq(scalaz7)
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val json_ext =
  coreProject("json-ext")
    .dependsOn(common, json)
    .settings(
      description := "Extentions to JSON Library",
      libraryDependencies ++= Seq(commons_codec, joda_time, joda_convert)
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val util =
  coreProject("util")
    .dependsOn(actor, json, markdown)
    .settings(
      description := "Utilities Library",
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(
        joda_time,
        joda_convert,
        commons_codec,
        javamail,
        log4j,
        htmlparser,
        xerces,
        jbcrypt
      )
    )
    .settings(crossScalaVersions := crossUpTo213)

// Web Projects
// ------------
lazy val web: Seq[ProjectReference] =
  Seq(testkit, webkit)

lazy val testkit =
  webProject("testkit")
    .dependsOn(util)
    .settings(
      description := "Testkit for Webkit Library",
      libraryDependencies ++= Seq(commons_httpclient, servlet_api)
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val webkit =
  webProject("webkit")
    .dependsOn(util, testkit % "provided")
    .settings(
      description := "Webkit Library",
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(
        commons_fileupload,
        rhino,
        servlet_api,
        specs2Prov,
        specs2MatchersProv,
        jetty6,
        jwebunit,
        mockito_scalatest,
        jquery,
        jasmineCore,
        jasmineAjax
      ),
      libraryDependencies ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, scalaMajor)) if scalaMajor >= 13 => Seq(scala_parallel_collections)
          case _ => Seq.empty
        }
      },
      initialize in Test := {
        System.setProperty(
          "net.liftweb.webapptest.src.test.webapp",
          ((sourceDirectory in Test).value / "webapp").absString
        )
      },
      unmanagedSourceDirectories in Compile += {
        (sourceDirectory in Compile).value / ("scala_" + scalaBinaryVersion.value)
      },
      unmanagedSourceDirectories in Test += {
        (sourceDirectory in Test).value / ("scala_" + scalaBinaryVersion.value)
      },
      compile in Compile := (compile in Compile).dependsOn(WebKeys.assets).value,
      /**
        * This is to ensure that the tests in net.liftweb.webapptest run last
        * so that other tests (MenuSpec in particular) run before the SiteMap
        * is set.
        */
      testGrouping in Test := {
        (definedTests in Test).map { tests =>
          import Tests._

          val (webapptests, others) = tests.partition { test =>
            test.name.startsWith("net.liftweb.webapptest")
          }

          Seq(
            new Group("others", others, InProcess),
            new Group("webapptests", webapptests, InProcess)
          )
        }.value
      },

    )
    .enablePlugins(SbtWeb)
    .settings(crossScalaVersions := crossUpTo213)

// Persistence Projects
// --------------------
lazy val persistence: Seq[ProjectReference] =
//  Seq(db, proto, mapper, record, squeryl_record, mongodb, mongodb_record)
// - I don't use mapper, record and mongodb.
// - squeryl_record can't be compiled in 2.13. Code must be updated for new
//   version of squeryl library first.
  Seq(db, proto)

lazy val db =
  persistenceProject("db")
    .dependsOn(util, webkit)
    .settings(libraryDependencies += mockito_scalatest)
    .settings(crossScalaVersions := crossUpTo213)

lazy val proto =
  persistenceProject("proto")
    .dependsOn(webkit)
    .settings(crossScalaVersions := crossUpTo213)

lazy val mapper =
  persistenceProject("mapper")
    .dependsOn(db, proto)
    .settings(
      description := "Mapper Library",
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(h2, derby, jbcrypt),
      initialize in Test := {
        System.setProperty(
          "derby.stream.error.file",
          ((crossTarget in Test).value / "derby.log").absolutePath
        )
      }
    )
    .settings(crossScalaVersions := crossUpTo213)

lazy val record =
  persistenceProject("record")
    .dependsOn(proto)
    .settings(libraryDependencies ++= Seq(jbcrypt))
    .settings(crossScalaVersions := crossUpTo213)

lazy val squeryl_record =
  persistenceProject("squeryl-record")
    .dependsOn(record, db)
    .settings(libraryDependencies ++= Seq(h2, squeryl))

lazy val mongodb =
  persistenceProject("mongodb")
    .dependsOn(json_ext, util)
    .settings(
      crossScalaVersions := crossUpTo213,
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(mongo_java_driver, mongo_java_driver_async),
      initialize in Test := {
        System.setProperty(
          "java.util.logging.config.file",
          ((resourceDirectory in Test).value / "logging.properties").absolutePath
        )
      }
    )
    .settings(
      // Because of one warning: While parsing annotations in
      // mongodb-driver-core-3.12.7.jar(com/mongodb/lang/Nullable.class): could
      // not find MAYBE in enum <none>.
      scalacOptions ~= ((opts) => opts.filterNot(Set("-Xfatal-warnings")))
    )

lazy val mongodb_record =
  persistenceProject("mongodb-record")
    .dependsOn(record, mongodb)
    .settings(
      crossScalaVersions := crossUpTo213,
      parallelExecution in Test := false
    )
    .settings(
      // Because of multiple warnings about using deprecated method instead of
      // BsonableField.
      scalacOptions ~= ((opts) => opts.filterNot(Set("-Xfatal-warnings")))
    )
