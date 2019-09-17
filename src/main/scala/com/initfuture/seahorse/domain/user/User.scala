package com.initfuture.seahorse.domain.user

import java.time.LocalDate

case class User(
  name: String,
  email: String,
  phone: String,
  openId: String,
  loginAt: Option[LocalDate],
  registerAt: Option[LocalDate],
  id: Option[Long] = None
)

object User {
  def apply(
    name: String,
    email: String,
    phone: String,
    openId: String,
    id: Option[Long]
  ): User = new User(name, email, phone, openId, Some(LocalDate.now()), Some(LocalDate.now()), id)
}
