package com.initfuture.seahorse.domain.user

import cats.data.OptionT

trait UserRepository[F[_]] {
  def create(user: User): F[User]

  def update(user: User): OptionT[F, User]

  def get(userId: Long): OptionT[F, User]

  def getByOpenId(openId: String): OptionT[F, User]

}
