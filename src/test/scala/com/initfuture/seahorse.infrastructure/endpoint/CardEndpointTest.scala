package com.initfuture.seahorse.infrastructure.endpoint

import cats.data.Kleisli
import cats.effect.{ ContextShift, Sync }
import com.initfuture.seahorse.domain.authentication.{ Authentication, AuthenticationInterpreter, TokenPayload }
import com.initfuture.seahorse.domain.card.{ Card, CardService, CardValidationInterpreter }
import com.initfuture.seahorse.infrastructure.repository.{ CardRepositoryInMemory, UserRepositoryInMemory }
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.util.CaseInsensitiveString
import org.specs2.mutable.Specification
import zio.interop.catz._
import zio.{ DefaultRuntime, Task }

class CardEndpointTest extends Specification {

  private val testRuntime                         = new DefaultRuntime {}
  implicit val encoder: EntityEncoder[Task, Card] = jsonEncoderOf[Task, Card]
  implicit val decoder: EntityDecoder[Task, Card] = jsonOf[Task, Card]
  private val tokenPrefix = "Bearer "

  def getRoute[F[_]: Sync](implicit C: ContextShift[F]): (Authentication[F], Kleisli[F, Request[F], Response[F]]) = {
    val repo                    = new CardRepositoryInMemory[F]()
    val validator               = new CardValidationInterpreter[F](repo)
    val service: CardService[F] = new CardService[F](repo, validator)

    val userRepo = new UserRepositoryInMemory[F]()
    val auth     = new AuthenticationInterpreter[F]("authtestkey", userRepo)
    (auth, CardEndpoint.endpoints(service, auth).orNotFound)
  }

  def makeToken[F[_]](auth: Authentication[F], payload: TokenPayload): String = auth.makeToken(payload)

  "Card tests" >> {
    "create should return a card" >> {
      val card           = Card(None, "It's a card")
      val (auth, routes) = getRoute[Task]
      val req = Request[Task](Method.POST, uri"/card")
        .withEntity(card)
        .withHeaders(Header.Raw(CaseInsensitiveString("Authorization"), tokenPrefix + makeToken(auth, TokenPayload(1L))))

      val resp = testRuntime.unsafeRun(routes.run(req))
      resp.status must beEqualTo(Status.Created)
      testRuntime.unsafeRun(resp.as[Card]).content must beEqualTo(card.content)
    }

    "return 200 and current card" in {
      val (auth, routes) = getRoute[Task]
      val request = Request[Task](Method.GET, uri"/card/1")
        .withHeaders(Header.Raw(CaseInsensitiveString("Authorization"), tokenPrefix + makeToken(auth, TokenPayload(1L))))
      val cardResponse =
        testRuntime.unsafeRun(routes.run(request))
      cardResponse.status must beEqualTo(Status.Ok)

      testRuntime.unsafeRun(cardResponse.as[Card]).content must beEqualTo(
        """It's a card"""
      )
    }
  }
}
