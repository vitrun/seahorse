package com.initfuture.seahorse.infrastructure.endpoint

import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import com.initfuture.seahorse.domain._
import com.initfuture.seahorse.domain.authentication.Authentication
import com.initfuture.seahorse.domain.card.{ Card, CardService }
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{ EntityDecoder, EntityEncoder, HttpRoutes }
import tapir.{ statusMapping, _ }
import tapir.json.circe._
import tapir.model.StatusCodes
import tapir.server.http4s._

object CardApi {

  val createCardEndpoint: Endpoint[(String, Card), ErrorResponse, Card, Nothing] = endpoint.post
    .in(auth.bearer)
    .in("card")
    .in(jsonBody[Card])
    .errorOut(
      oneOf[ErrorResponse](
        statusMapping(StatusCodes.InternalServerError, jsonBody[InternalErrorResp]),
        statusMapping(StatusCodes.Conflict, jsonBody[AlreadyExistResp]),
        statusMapping(StatusCodes.Unauthorized, jsonBody[UnauthorizedErrorResp])
      )
    )
    .out(
      oneOf(
        statusMapping(StatusCodes.Created, jsonBody[Card])
      )
    )

  val getCardEndPoint = endpoint.get
    .in(auth.bearer)
    .in("card")
    .in(path[Long]("id").validate(Validator.min(1)))
    .errorOut(
      oneOf(
        statusMapping(StatusCodes.NotFound, jsonBody[NotFoundErrorResp]),
        statusMapping(StatusCodes.InternalServerError, jsonBody[InternalErrorResp]),
        statusMapping(StatusCodes.Unauthorized, jsonBody[UnauthorizedErrorResp])
      )
    )
    .out(jsonBody[Card])

}

class CardEndpoint[F[_]: Sync](cardService: CardService[F], auth: Authentication[F])(
  implicit serverOptions: Http4sServerOptions[F],
  fcs: ContextShift[F]
) extends Http4sDsl[F] {
  implicit val decoder: EntityDecoder[F, Card] = jsonOf[F, Card]
  implicit val encoder: EntityEncoder[F, Card] = jsonEncoderOf[F, Card]

  private def createCardLogic(token: Token, card: Card): F[Either[ErrorResponse, Card]] =
    auth.authUser(token) { _ =>
      cardService
        .create(card)
        .bimap[ErrorResponse, Card](existError => AlreadyExistResp(s"$existError"), card => card)
    }

  private def getCardLogic(token: Token, id: Long): F[Either[ErrorResponse, Card]] =
    auth.authUser(token) { _ =>
      cardService
        .get(id)
        .bimap[ErrorResponse, Card](_ => NotFoundErrorResp(s"$id not found"), card => card)
    }

  def endpoints(): HttpRoutes[F] =
    CardApi.createCardEndpoint.toRoutes((createCardLogic _).tupled) <+>
      CardApi.getCardEndPoint.toRoutes((getCardLogic _).tupled)
}

object CardEndpoint {

  def endpoints[F[_]: Sync](service: CardService[F], auth: Authentication[F])(
    implicit serverOptions: Http4sServerOptions[F],
    fcs: ContextShift[F]
  ): HttpRoutes[F] = new CardEndpoint[F](service, auth).endpoints()

  val apis = List(CardApi.createCardEndpoint, CardApi.getCardEndPoint)
}
