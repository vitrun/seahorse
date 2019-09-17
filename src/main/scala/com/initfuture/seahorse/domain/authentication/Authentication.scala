package com.initfuture.seahorse.domain.authentication

import cats.data.EitherT
import com.initfuture.seahorse.domain.ErrorResponse
import com.initfuture.seahorse.domain.user.User

import scala.util.Try

trait Authentication[F[_]] {
  type Token = String

  def decodeToken(token: Token): Try[(String, String, String)]

  def makeToken(payload: TokenPayload): Token

  def verifyToken(token: Token): EitherT[F, ErrorResponse, User]

  def authUser[I, O](token: Token)(f: User => EitherT[F, ErrorResponse, O]): F[Either[ErrorResponse, O]]
}
