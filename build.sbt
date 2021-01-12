val postgresqlVersion = "42.2.18"

name := """play-payment"""
organization := "ru.juliomoralez"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.postgresql" % "postgresql" % postgresqlVersion
libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.2"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
