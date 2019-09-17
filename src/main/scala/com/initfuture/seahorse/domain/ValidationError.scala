package com.initfuture.seahorse.domain

sealed trait ValidationError extends Product with Serializable

case class NotExistError(error: String) extends ValidationError

case class AlreadyExistError(error: String) extends ValidationError
