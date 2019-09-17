package com.initfuture.seahorse.infrastructure.repository

import cats.Applicative
import cats.data.OptionT
import cats.implicits._
import com.initfuture.seahorse.domain.user.{ User, UserRepository }

import scala.util.Random

class UserRepositoryInMemory[F[_]: Applicative] extends UserRepository[F] {
  override def create(user: User): F[User] = {
    val newUser = user.copy(id = user.id orElse new Random().nextLong(20).some)
    newUser.pure[F]
  }

  override def get(id: Long): OptionT[F, User] =
    if (id == 1L) OptionT(User("Alex", "It's a user", "341341", "unknown", Some(1L)).some.pure[F])
    else OptionT((None: Option[User]).pure[F])

  override def update(user: User): OptionT[F, User] = ???

  override def getByOpenId(openId: String): OptionT[F, User] =
    OptionT(User("Alex", "It's a user", "341341", "unknown", Some(1L)).some.pure[F])
}
