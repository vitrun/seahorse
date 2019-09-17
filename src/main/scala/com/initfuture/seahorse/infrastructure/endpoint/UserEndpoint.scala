package com.initfuture.seahorse.infrastructure.endpoint

import java.time.LocalDate

import cats.effect.{ ConcurrentEffect, ContextShift, IO, Sync, SyncIO }
import cats.implicits._
import com.initfuture.seahorse.domain.authentication.{ Authentication, LoginRequest, TokenPayload }
import com.initfuture.seahorse.domain.user.{ User, UserService }
import com.initfuture.seahorse.domain.{ ErrorResponse, InternalErrorResp, UnauthorizedErrorResp }
import com.initfuture.seahorse.external.wechat.{ WxError, WxResp, WxSession }
import com.initfuture.seahorse.infrastructure.external.wechat.WechatServiceInterpreter
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.log4s.getLogger
import tapir.json.circe._
import tapir.model.StatusCodes
import tapir.server.http4s._
import tapir.{ statusMapping, _ }

import scala.concurrent.ExecutionContext.global

object UserApi {

  val loginEndpoint: Endpoint[LoginRequest, ErrorResponse, TokenPayload, Nothing] = endpoint.post
    .in("user" / "login")
    .in(jsonBody[LoginRequest])
    .errorOut(
      oneOf[ErrorResponse](
        statusMapping(StatusCodes.InternalServerError, jsonBody[InternalErrorResp]),
        statusMapping(StatusCodes.Unauthorized, jsonBody[UnauthorizedErrorResp])
      )
    )
    .out(
      oneOf(
        statusMapping(StatusCodes.Ok, jsonBody[TokenPayload])
      )
    )
}

class UserEndpoint[F[_]: Sync, G[_]: ConcurrentEffect](
  userService: UserService[F],
  wechatService: WechatServiceInterpreter[G],
  auth: Authentication[F]
)(
  implicit serverOptions: Http4sServerOptions[F],
  fcs: ContextShift[F]
) extends Http4sDsl[F] {
  implicit val shift: ContextShift[IO] = IO.contextShift(global)

  private val logger = getLogger

  def loginLogic(loginReq: LoginRequest): F[Either[ErrorResponse, TokenPayload]] = {
    logger.info(s"got login request: $loginReq")

    val effect = implicitly[ConcurrentEffect[G]].toIO(wechatService.code2session(loginReq.code))
    implicitly[Sync[F]].delay(effect.unsafeRunSync()).flatMap {
      case Left(e) =>
        Either.left[ErrorResponse, TokenPayload](e).pure[F]
      case Right(wxResp) =>
        wxResp match {
          case WxError(_, errorMsg) =>
            Either.left[ErrorResponse, TokenPayload](InternalErrorResp(errorMsg)).pure[F]
          case session @ WxSession(_, _, _) => onSessionReceived(session)
        }
    }
  }

  private def onSessionReceived(session: WxSession) =
    userService.getUserByOpenId(session.openId).value.map {
      case Left(_) =>
        // register the user
        val user = User("unknown", "unknown", "unknown", session.openId, None)
        val _    = userService.createUser(user)
        user.id
          .map(TokenPayload)
          .toRight[ErrorResponse](InternalErrorResp(s"no user for openId ${session.openId}"))
      case Right(user) =>
        user.id match {
          case None =>
            Either.left(InternalErrorResp(s"user for ${session.openId} has no id!"))
          case Some(id) =>
            // update the login time
            userService.update(user.copy(loginAt = Some(LocalDate.now())))
            Either.right(TokenPayload(id))
        }
    }

  def endpoints(): HttpRoutes[F] = UserApi.loginEndpoint.toRoutes(loginLogic)
}

object UserEndpoint {

  def endpoints[F[_]: Sync, G[_]: ConcurrentEffect](
    userService: UserService[F],
    wechatService: WechatServiceInterpreter[G],
    auth: Authentication[F]
  )(
    implicit serverOptions: Http4sServerOptions[F],
    fcs: ContextShift[F]
  ): HttpRoutes[F] = new UserEndpoint[F, G](userService, wechatService, auth).endpoints()

  val apis = List(UserApi.loginEndpoint)
}
