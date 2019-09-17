package com.initfuture.seahorse.config

case class DatabaseConnectionsConfig(poolSize: Int)
case class DatabaseConfig(
  url: String,
  driver: String,
  user: String,
  password: String,
  connections: DatabaseConnectionsConfig
)
