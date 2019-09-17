package com.initfuture.seahorse.domain.card

import cats.data.EitherT
import com.initfuture.seahorse.domain.AlreadyExistError

trait CardValidationAlgebra[F[_]] {
  def doesNotExist(card: Card): EitherT[F, AlreadyExistError, Card]
}
