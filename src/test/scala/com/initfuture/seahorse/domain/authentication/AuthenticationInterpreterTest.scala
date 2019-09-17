package com.initfuture.seahorse.domain.authentication

import cats.effect.{ ContextShift, Sync }
import com.initfuture.seahorse.infrastructure.repository.UserRepositoryInMemory
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.specs2.mutable.Specification
import pdi.jwt.{ Jwt, JwtAlgorithm }
import zio.interop.catz._
import zio.{ DefaultRuntime, Task }

import scala.util.{ Failure, Success }

class AuthenticationInterpreterTest extends Specification {
  private val testRuntime = new DefaultRuntime {}

  val key = "aaa"
  val uid = 1L

  def getAuth[F[_]: Sync](implicit C: ContextShift[F]): Authentication[F] = {
    val userRepo = new UserRepositoryInMemory[F]()
    new AuthenticationInterpreter[F](key, userRepo)
  }

  "AuthenticationInterpreterTest" >> {
    "Token verify" >> {
      val token = getAuth.makeToken(TokenPayload(1L))
      testRuntime.unsafeRun(getAuth[Task].verifyToken(token).value) match {
        case Left(_)     => ko
        case Right(user) => user.id must beSome(uid)
      }
    }

    "Token encode and decode" >> {
      val token   = Jwt.encode(TokenPayload(uid).asJson.noSpaces, key, JwtAlgorithm.HS256)
      val payload = Jwt.decodeRawAll(token, key, Seq(JwtAlgorithm.HS256))
      payload match {
        case Failure(_) => ko
        case Success(tuple) =>
          decode[TokenPayload](tuple._2) match {
            case Left(_)   => ko
            case Right(pl) => pl.uid must_== uid
          }
      }
    }
  }
}
