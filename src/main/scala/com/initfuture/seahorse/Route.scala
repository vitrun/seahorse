package com.initfuture.seahorse

import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import com.initfuture.seahorse.domain.authentication.AuthenticationInterpreter
import com.initfuture.seahorse.domain.card.{CardService, CardValidationInterpreter}
import com.initfuture.seahorse.domain.user.{UserService, UserValidationInterpreter}
import com.initfuture.seahorse.infrastructure.endpoint.{CardEndpoint, UserEndpoint}
import com.initfuture.seahorse.infrastructure.external.wechat.WechatServiceInterpreter
import com.initfuture.seahorse.infrastructure.repository.{CardRepositoryInterpreter, UserRepositoryInterpreter}
import doobie.util.transactor.Transactor
import org.http4s.HttpRoutes
import org.http4s.server.Router
import tapir.server.http4s.Http4sServerOptions
import tapir.swagger.http4s.SwaggerHttp4s
import tapir.docs.openapi._
import tapir.openapi.circe.yaml._

object Route {

  def cardRoutes[F[_]: Sync](trx: Transactor[F], authenticator: AuthenticationInterpreter[F])(
    implicit serverOptions: Http4sServerOptions[F],
    fcs: ContextShift[F]
  ): HttpRoutes[F] = {
    val repo                    = new CardRepositoryInterpreter[F](trx)
    val validator               = new CardValidationInterpreter[F](repo)
    val service: CardService[F] = new CardService[F](repo, validator)
    CardEndpoint.endpoints(service, authenticator)
  }

  def userRoutes[F[_]: Sync, G[_]: ConcurrentEffect](trx: Transactor[F], authenticator: AuthenticationInterpreter[F])(
    implicit serverOptions: Http4sServerOptions[F],
    fcs: ContextShift[F]
  ): HttpRoutes[F] = {
    val repo          = new UserRepositoryInterpreter[F](trx)
    val validator     = new UserValidationInterpreter[F](repo)
    val service       = new UserService[F](repo, validator)
    val wechatService = WechatServiceInterpreter[G]
    UserEndpoint.endpoints(service, wechatService, authenticator)
  }

  def allRoutes[F[_]: ConcurrentEffect](trx: Transactor[F], key: String)(
    implicit serverOptions: Http4sServerOptions[F],
    fcs: ContextShift[F]
  ): HttpRoutes[F] = {
    val yaml = (CardEndpoint.apis ++ UserEndpoint.apis).toOpenAPI("seahorse", "1.0").toYaml

    val userRepo      = new UserRepositoryInterpreter[F](trx)
    val authenticator = new AuthenticationInterpreter(key, userRepo)

    Router(
      "/docs" -> new SwaggerHttp4s(yaml).routes,
      "/"     -> userRoutes[F, F](trx, authenticator),
      "/"     -> cardRoutes(trx, authenticator)
    )
  }
}
