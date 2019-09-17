package com.initfuture.seahorse.domain.card

case class Card(id: Option[Long] = None, content: String, tags: Set[String] = Set.empty)
