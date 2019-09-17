package com.initfuture.seahorse.domain.authentication

import java.time.LocalDate

import cats.data.EitherT
import cats.implicits._
import cats.effect.Sync
import com.initfuture.seahorse.domain.user.{ User, UserRepository }
import com.initfuture.seahorse.domain.{ ErrorResponse, UnauthorizedErrorResp }
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import pdi.jwt.{ Jwt, JwtAlgorithm }

import scala.util.{ Failure, Success, Try }

class AuthenticationInterpreter[F[_]: Sync](key: String, userRepo: UserRepository[F]) extends Authentication[F] {

  override def verifyToken(token: Token): EitherT[F, ErrorResponse, User] =
    decodeToken(token) match {
      case Failure(exception) =>
        val e: ErrorResponse = UnauthorizedErrorResp(s"$exception")
        EitherT.left[User](e.pure[F])
      case Success(tuple) =>
        decode[TokenPayload](tuple._2) match {
          case Left(e) =>
            val err: ErrorResponse = UnauthorizedErrorResp(s"$e")
            EitherT.left(err.pure[F])
          case Right(payload) =>
            //check if token is expired
            val err: ErrorResponse = UnauthorizedErrorResp(s"no such user")
            userRepo
              .get(payload.uid)
              .toRight(err)
              .ensure(UnauthorizedErrorResp(s"token is expired")) { user =>
                user.loginAt.isDefined && LocalDate.now().minusDays(3).isBefore(user.loginAt.get)
              }
        }
    }

  override def authUser[I, O](token: Token)(
    f: User => EitherT[F, ErrorResponse, O]
  ): F[Either[ErrorResponse, O]] =
    (for {
      user <- verifyToken(token)
      r    <- f(user)
    } yield r).value

  override def decodeToken(token: Token): Try[(String, String, String)] =
    Jwt.decodeRawAll(token, key, Seq(JwtAlgorithm.HS256))

  override def makeToken(payload: TokenPayload): Token =
    Jwt.encode(payload.asJson.noSpaces, key, JwtAlgorithm.HS256)
}
