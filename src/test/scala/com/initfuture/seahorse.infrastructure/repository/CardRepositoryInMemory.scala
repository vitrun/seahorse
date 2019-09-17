package com.initfuture.seahorse.infrastructure.repository

import cats.Applicative
import cats.implicits._
import com.initfuture.seahorse.domain.card.{ Card, CardRepository }

import scala.util.Random

class CardRepositoryInMemory[F[_]: Applicative] extends CardRepository[F] {
  override def create(card: Card): F[Card] = {
    val newCard = card.copy(id = card.id orElse new Random().nextLong(20).some)
    newCard.pure[F]
  }

  override def get(id: Long): F[Option[Card]] =
    if (id == 1L) Card(id.some, "It's a card").some.pure[F]
    else (None: Option[Card]).pure[F]
}
