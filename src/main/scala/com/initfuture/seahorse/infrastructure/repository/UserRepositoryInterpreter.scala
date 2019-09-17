package com.initfuture.seahorse.infrastructure.repository

import cats.data.OptionT
import cats.effect.Bracket
import cats.implicits._
import com.initfuture.seahorse.domain.user.{ User, UserRepository }
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import doobie.util.{ Get, Put }

class UserRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](trx: Transactor[F]) extends UserRepository[F] {
  override def create(user: User): F[User] =
    UserSql.insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(trx)

  override def get(id: Long): OptionT[F, User] = {
    val x = UserSql.select(id).option.transact(trx)
    OptionT(x)
  }

  override def update(user: User): OptionT[F, User] =
    OptionT.fromOption[F](user.id).semiflatMap { id =>
      UserSql.update(user, id).run.transact(trx).as(user)
    }

  override def getByOpenId(openId: String): OptionT[F, User] = {
    val x = UserSql.selectByOpenId(openId).option.transact(trx)
    OptionT(x)
  }
}

private object UserSql {
  def fromStr(s: String): Set[String] = s.split(",").toSet
  def toStr(s: Set[String]): String   = s.mkString(",")

  implicit val tagsGet: Get[Set[String]] = Get[String].map(fromStr)
  implicit val tagsPut: Put[Set[String]] = Put[String].contramap(toStr)

  def selectByOpenId(openId: String): Query0[User] = sql"""
    id, name, open_id FROM uer_user 
    WHERE open_id = $openId
    """.query[User]

  def select(userId: Long): Query0[User] = sql"""
    SELECT id, name
    FROM user_user
    WHERE id = $userId
  """.query[User]

  def insert(user: User): Update0 = sql"""
    INSERT INTO user_user (name, phone) VALUES (${user.name}, ${user.phone})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE user_user
    SET name = ${user.name}, phone = ${user.phone},
        login_at = ${user.loginAt}
    WHERE ID = $id
  """.update

}
