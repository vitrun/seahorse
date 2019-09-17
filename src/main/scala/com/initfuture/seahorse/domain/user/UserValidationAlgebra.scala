package com.initfuture.seahorse.domain.user

import cats.data.EitherT
import com.initfuture.seahorse.domain.{AlreadyExistError, NotExistError}

trait UserValidationAlgebra[F[_]] {
  def doesNotExist(user: User): EitherT[F, AlreadyExistError, Unit]

  def exists(userId: Option[Long]): EitherT[F, NotExistError, Unit]
}
