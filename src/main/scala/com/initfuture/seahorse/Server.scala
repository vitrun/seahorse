package com.initfuture.seahorse

import cats.effect.{ Async, Blocker, ConcurrentEffect, ContextShift, Resource, Timer }
import com.initfuture.seahorse.config.{ AuthConfig, DatabaseConfig, ServerConfig }
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import io.circe.config.parser
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

object Server {

  def dbTransactor[F[_]: Async: ContextShift](
    dbc: DatabaseConfig,
    connEc: ExecutionContext,
    blocker: Blocker
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      dbc.driver,
      dbc.url,
      dbc.user,
      dbc.password,
      connEc,
      blocker
    )

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]) =
    for {
      dbConf     <- Resource.liftF(parser.decodePathF[F, DatabaseConfig]("all.db"))
      conn       <- ExecutionContexts.fixedThreadPool(dbConf.connections.poolSize)
      txnEc      <- ExecutionContexts.cachedThreadPool[F]
      trx        <- dbTransactor(dbConf, conn, Blocker.liftExecutionContext(txnEc))
      serverConf <- Resource.liftF(parser.decodePathF[F, ServerConfig]("all.server"))
      keyConf    <- Resource.liftF(parser.decodePathF[F, AuthConfig]("all.auth"))

      httpApp      = Route.allRoutes(trx, keyConf.key).orNotFound
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      server <- BlazeServerBuilder[F]
                 .bindHttp(serverConf.port, serverConf.host)
                 .withHttpApp(finalHttpApp)
                 .resource
    } yield server
}
