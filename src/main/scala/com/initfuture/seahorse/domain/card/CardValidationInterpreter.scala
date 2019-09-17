package com.initfuture.seahorse.domain.card

import cats.Applicative
import cats.data.EitherT
import cats.implicits._
import com.initfuture.seahorse.domain.AlreadyExistError

class CardValidationInterpreter[F[_]: Applicative](repo: CardRepository[F]) extends CardValidationAlgebra[F] {
  override def doesNotExist(card: Card): EitherT[F, AlreadyExistError, Card] = EitherT {
    card.id.map(repo.get) match {
      case Some(_) => Either.left[AlreadyExistError, Card](AlreadyExistError("$card.id already exists")).pure[F]
      case _       => Either.right[AlreadyExistError, Card](card).pure[F]
    }
  }
}
