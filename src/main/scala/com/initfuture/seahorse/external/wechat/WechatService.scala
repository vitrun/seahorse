package com.initfuture.seahorse.external.wechat

import com.initfuture.seahorse.domain.ErrorResponse

trait WechatService[F[_]] {

  def code2session(code: String): F[Either[ErrorResponse, WxResp]]
}
