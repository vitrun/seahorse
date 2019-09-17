val Http4sV        = "0.21.0-M2"
val CirceV         = "0.12.0-M4"
val CirceConfigV   = "0.7.0-M1"
val Specs2V        = "4.6.0"
val LogbackV       = "1.2.3"
val ZioV           = "1.0.0-RC10-1"
val ZioInteropCats = "2.0.0.0-RC1"
val ScalaCheckV    = "1.14.0"
val AmmoniteV      = "1.6.9-15-6720d42"
val MysqlConnV     = "8.0.15"
val DoobieV        = "0.8.0-M3"
val TapirV         = "0.10.1"
val JwtV           = "4.0.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.initfuture",
    name := "seahorse",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.0",
    //scala  libraries
    libraryDependencies ++= Seq(
      "org.http4s"             %% "http4s-blaze-server"      % Http4sV,
      "org.http4s"             %% "http4s-blaze-client"      % Http4sV,
      "org.http4s"             %% "http4s-circe"             % Http4sV,
      "org.http4s"             %% "http4s-dsl"               % Http4sV,
      "dev.zio"                %% "zio"                      % ZioV,
      "dev.zio"                %% "zio-interop-cats"         % ZioInteropCats,
      "io.circe"               %% "circe-generic"            % CirceV,
      "io.circe"               %% "circe-config"             % CirceConfigV,
      "org.tpolecat"           %% "doobie-core"              % DoobieV,
      "org.tpolecat"           %% "doobie-hikari"            % DoobieV,
      "org.tpolecat"           %% "doobie-specs2"            % DoobieV,
      "com.softwaremill.tapir" %% "tapir-http4s-server"      % TapirV,
      "com.softwaremill.tapir" %% "tapir-json-circe"         % TapirV,
      "com.softwaremill.tapir" %% "tapir-swagger-ui-http4s"  % TapirV,
      "com.softwaremill.tapir" %% "tapir-openapi-docs"       % TapirV,
      "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % TapirV,
      "org.specs2"             %% "specs2-core"              % Specs2V % Test,
      "org.scalacheck"         %% "scalacheck"               % ScalaCheckV % Test,
      "com.pauldijou"          %% "jwt-core"                 % JwtV,
      "org.specs2"             %% "specs2-mock"              % Specs2V % Test
    ),
    //java libraries
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic"      % LogbackV,
      "com.lihaoyi"    % "ammonite"             % AmmoniteV % "test" cross CrossVersion.full,
      "mysql"          % "mysql-connector-java" % MysqlConnV
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings"
)

// To activate the Ammonite REPL: sbt projectName/test:run-main amm
sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue
