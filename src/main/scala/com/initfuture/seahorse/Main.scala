package com.initfuture.seahorse

import zio.{ App, Task, ZIO }
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends App {
  override def run(args: List[String]): ZIO[Environment, Nothing, Int] =
    ZIO
      .runtime[Environment]
      .flatMap(
        implicit rts => Server.stream[Task].use(_ => ZIO.never).either.fold(_ => 1, _ => 0)
      )
}
