package com.initfuture.seahorse.domain.user

import cats.Applicative
import cats.data.EitherT
import cats.implicits._
import com.initfuture.seahorse.domain.{AlreadyExistError, NotExistError}

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepository[F]) extends UserValidationAlgebra[F] {

  def exists(userId: Option[Long]): EitherT[F, NotExistError, Unit] =
    userId match {
      case Some(id) =>
        userRepo.get(id)
          .toRight(NotExistError("$id not exist"))
          .void
      case None =>
        EitherT.left[Unit](NotExistError("").pure[F])
    }

  override def doesNotExist(user: User): EitherT[F, AlreadyExistError, Unit] = ???
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](repo: UserRepository[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](repo)
}
