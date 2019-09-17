package com.initfuture.seahorse.domain.card

trait CardRepository[F[_]] {
  def create(card: Card): F[Card]

  def get(id: Long): F[Option[Card]]

}
