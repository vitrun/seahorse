package com.initfuture.seahorse.infrastructure.endpoint

import cats.data.Kleisli
import cats.implicits._
import cats.effect.{ ConcurrentEffect, ContextShift, IO }
import com.initfuture.seahorse.domain.ErrorResponse
import com.initfuture.seahorse.domain.authentication.{
  Authentication,
  AuthenticationInterpreter,
  LoginRequest,
  TokenPayload
}
import com.initfuture.seahorse.domain.user.{ UserService, UserValidationInterpreter }
import com.initfuture.seahorse.external.wechat.{ WxResp, WxSession }
import com.initfuture.seahorse.infrastructure.external.wechat.WechatServiceInterpreter
import com.initfuture.seahorse.infrastructure.repository.UserRepositoryInMemory
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.global

class UserEndpointTest extends Specification with Mockito {

  def getRoute[F[_]: ConcurrentEffect](
    implicit C: ContextShift[F]
  ): (Authentication[F], Kleisli[F, Request[F], Response[F]]) = {
    val repo                    = new UserRepositoryInMemory[F]()
    val validator               = new UserValidationInterpreter[F](repo)
    val service: UserService[F] = new UserService[F](repo, validator)

    val userRepo      = new UserRepositoryInMemory[F]()
    val auth          = new AuthenticationInterpreter[F]("authtestkey", userRepo)
    val wechatService = mock[WechatServiceInterpreter[F]]
    wechatService.code2session(any[String]()) returns Either
      .right[ErrorResponse, WxResp](WxSession("openId", "key", 7200))
      .pure[F]

    (auth, UserEndpoint.endpoints(service, wechatService, auth).orNotFound)
  }

  implicit val shift: ContextShift[IO] = IO.contextShift(global)

  implicit val encoder: EntityEncoder[IO, LoginRequest] = jsonEncoderOf
  implicit val decoder: EntityDecoder[IO, TokenPayload] = jsonOf

  "User tests" >> {
    "login ok" >> {
      val (auth, routes) = getRoute[IO]
      val req = Request[IO](Method.POST, uri"/user/login")
        .withEntity(LoginRequest("code"))

      val res = routes.run(req).unsafeRunSync()
      res.status must beEqualTo(Status.Ok)
      res.as[TokenPayload].unsafeRunSync().uid must beGreaterThan(0L)
    }
  }
}
