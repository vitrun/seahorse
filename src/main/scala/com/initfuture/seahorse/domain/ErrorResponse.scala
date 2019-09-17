package com.initfuture.seahorse.domain

sealed trait ErrorResponse                        extends Product with Serializable
case class InternalErrorResp(message: String)     extends ErrorResponse
case class NotFoundErrorResp(message: String)     extends ErrorResponse
case class AlreadyExistResp(message: String)      extends ErrorResponse
case class UnauthorizedErrorResp(message: String) extends ErrorResponse
case class InvalidConfErrorResp(message: String)  extends ErrorResponse
