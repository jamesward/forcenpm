name := "forcenpm"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

incOptions := incOptions.value.withNameHashing(true)

updateOptions := updateOptions.value.withCachedResolution(true)

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  ws,
  filters,

  "com.jsuereth"    %% "scala-arm" % "1.4",

  "org.webjars"     %% "webjars-play" % "2.5.0-2",

  "org.webjars.npm" % "angular__common" % "2.0.0-rc.4",
  "org.webjars.npm" % "angular__compiler" % "2.0.0-rc.4",
  "org.webjars.npm" % "angular__core" % "2.0.0-rc.4",
  "org.webjars.npm" % "angular__http" % "2.0.0-rc.4",
  "org.webjars.npm" % "angular__platform-browser" % "2.0.0-rc.4",
  "org.webjars.npm" % "angular__platform-browser-dynamic" % "2.0.0-rc.4",
  "org.webjars.npm" % "angular__router" % "3.0.0-beta.1",
  "org.webjars.npm" % "angular__upgrade" % "2.0.0-rc.4",

  "org.webjars.npm" % "ng2-auto-complete" % "0.4.1",

  "org.webjars.npm" % "systemjs" % "0.19.35",
  "org.webjars.npm" % "core-js" % "2.4.1",
  "org.webjars.npm" % "reflect-metadata" % "0.1.3",
  "org.webjars.npm" % "rxjs" % "5.0.0-beta.10",
  "org.webjars.npm" % "zone.js" % "0.6.12",

  "org.webjars.npm" % "tslint-eslint-rules" % "1.2.0",
  "org.webjars.npm" % "codelyzer" % "0.0.19",

  "org.webjars"     % "salesforce-lightning-design-system" % "2.0.2"
)

pipelineStages := Seq(digest, gzip)

resolveFromWebjarsNodeModulesDir := true

tsCodesToIgnore := List(canNotFindModule)
