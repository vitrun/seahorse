package com.initfuture.seahorse.domain.user

import cats.{Functor, Monad}
import cats.data._
import com.initfuture.seahorse.domain.{NotExistError, ValidationError}

class UserService[F[_]](userRepo: UserRepository[F], validation: UserValidationAlgebra[F]) {
  def getUserByOpenId(openId: String)(implicit F: Functor[F]): EitherT[F, ValidationError, User] =
    userRepo.getByOpenId(openId).toRight(NotExistError(""))

  def createUser(user: User)(implicit M: Monad[F]): EitherT[F, ValidationError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved

  def getUser(userId: Long)(implicit F: Functor[F]): EitherT[F, ValidationError, User] =
    userRepo.get(userId).toRight(NotExistError(""))

  def update(user: User)(implicit M: Monad[F]): EitherT[F, ValidationError, User] =
    for {
      _ <- validation.exists(user.id)
      error: ValidationError = NotExistError("")
      saved <- userRepo.update(user).toRight(error)
    } yield saved
}

object UserService {
  def apply[F[_]](repository: UserRepository[F], validation: UserValidationAlgebra[F]): UserService[F] =
    new UserService[F](repository, validation)
}
