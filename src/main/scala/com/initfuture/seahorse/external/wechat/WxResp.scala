package com.initfuture.seahorse.external.wechat

import cats.effect.IO
import cats.syntax.functor._
import io.circe.{Decoder, HCursor}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

sealed trait WxResp
case class WxSession(openId: String, sessionKey: String, expiresIn: Long) extends WxResp
case class WxError(errorCode: Int, errorMsg: String)                      extends WxResp

object WxResp {
  implicit val sessionDecoder: Decoder[WxSession] = (c: HCursor) =>
    for {
      openId     <- c.downField("openid").as[String]
      sessionKey <- c.downField("session_key").as[String]
      expiresIn  <- c.downField("expires_in").as[Long]
    } yield WxSession(openId, sessionKey, expiresIn)

  implicit val errorDecoder: Decoder[WxError] = (c: HCursor) =>
    for {
      errorMsg  <- c.downField("errmsg").as[String]
      errorCode <- c.downField("errcode").as[Int]
    } yield WxError(errorCode, errorMsg)

  implicit val decodeWxResp: Decoder[WxResp] =
    List[Decoder[WxResp]](
      Decoder[WxSession].widen,
      Decoder[WxError].widen
    ).reduceLeft(_ or _)

  implicit val respDecoder: EntityDecoder[IO, WxResp] = jsonOf[IO, WxResp]
}
