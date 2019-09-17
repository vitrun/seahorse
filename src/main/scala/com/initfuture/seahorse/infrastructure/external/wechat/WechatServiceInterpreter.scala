package com.initfuture.seahorse.infrastructure.external.wechat

import cats.effect.ConcurrentEffect
import cats.implicits._
import com.initfuture.seahorse.domain.{ ErrorResponse, InternalErrorResp, InvalidConfErrorResp }
import com.initfuture.seahorse.external.wechat.{ WechatService, WxResp }
import org.http4s.Method.POST
import org.http4s.circe.jsonOf
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{ EntityDecoder, Request, Status, Uri }
import org.log4s.getLogger

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class WechatServiceInterpreter[F[_]: ConcurrentEffect] extends WechatService[F] {
  private val logger                                 = getLogger
  implicit val respDecoder: EntityDecoder[F, WxResp] = jsonOf[F, WxResp]

  private val wxSecret = "secret"
  private val wxId     = "appid"
  val wxApi =
    s"https://api.weixin.qq.com/sns/jscode2session?appid=$wxId&secret=$wxSecret&js_code=%s&grant_type=authorization_code"

  override def code2session(code: String): F[Either[ErrorResponse, WxResp]] =
    Uri.fromString(wxApi.format(code)).map(uri => Request[F](POST, uri)) match {
      case Left(_) =>
        Either.left[ErrorResponse, WxResp](InvalidConfErrorResp("invalid weixin session api")).pure[F]
      case Right(request) =>
        BlazeClientBuilder[F](global).withRequestTimeout(20.seconds).resource.use { client =>
          {
            client.fetch(request) {
              case Status.Successful(r) =>
                r.attemptAs[WxResp].value.map {
                  case Left(rr) =>
                    val error = s"failed submitting to weixin, body: $rr"
                    logger.warn(error)
                    Either.left[ErrorResponse, WxResp](InternalErrorResp(error))
                  case Right(resp) => Either.right(resp)
                }
              case r =>
                val error = s"failed submitting to weixin: ${r.status.code}, body: $r"
                logger.warn(error)
                Either.left[ErrorResponse, WxResp](InternalErrorResp(error)).pure[F]
            }
          }
        }
    }
}

object WechatServiceInterpreter {
  def apply[F[_]: ConcurrentEffect]: WechatServiceInterpreter[F] = new WechatServiceInterpreter[F]()
}
