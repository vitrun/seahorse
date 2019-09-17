package com.initfuture.seahorse.domain.card

import cats.{ Functor, Monad }
import cats.data.EitherT
import com.initfuture.seahorse.domain.{ AlreadyExistError, NotExistError }

class CardService[F[_]](repository: CardRepository[F], validator: CardValidationAlgebra[F]) {

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, NotExistError.type, Card] =
    EitherT.fromOptionF(repository.get(id), NotExistError)

  def create(card: Card)(implicit M: Monad[F]): EitherT[F, AlreadyExistError, Card] =
    for {
      _     <- validator.doesNotExist(card)
      saved <- EitherT.liftF(repository.create(card))
    } yield saved
}

object CardService {
  def apply[F[_]](repository: CardRepository[F], validator: CardValidationAlgebra[F]): CardService[F] =
    new CardService(repository, validator)
}
