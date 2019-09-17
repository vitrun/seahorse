package com.initfuture.seahorse.infrastructure.repository

import cats.effect.Bracket
import cats.implicits._
import com.initfuture.seahorse.domain.card.{ Card, CardRepository }
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import doobie.util.{ Get, Put }

class CardRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](trx: Transactor[F]) extends CardRepository[F] {
  override def create(card: Card): F[Card] =
    CardSql.insert(card).withUniqueGeneratedKeys[Long]("ID").map(id => card.copy(id = id.some)).transact(trx)

  override def get(id: Long): F[Option[Card]] = CardSql.select(id).option.transact(trx)

}

private object CardSql {

  def fromStr(s: String): Set[String] = s.split(",").toSet
  def toStr(s: Set[String]): String   = s.mkString(",")

  implicit val tagsGet: Get[Set[String]] = Get[String].map(fromStr)
  implicit val tagsPut: Put[Set[String]] = Put[String].contramap(toStr)

  def select(cardId: Long): Query0[Card] = sql"""
    SELECT id, card, tags
    FROM card_card
    WHERE id = $cardId
  """.query[Card]

  def insert(card: Card): Update0 = sql"""
    INSERT INTO card_card (card, tags) VALUES (${card.content}, ${card.tags})
  """.update
}
