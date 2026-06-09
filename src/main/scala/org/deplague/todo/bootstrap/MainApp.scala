package org.deplague.todo.bootstrap

import zio.Scope
import zio.ZIO
import zio.ZIOAppArgs
import zio.ZIOAppDefault
import zio.http.Response
import zio.http.Routes
import zio.http.Server

object MainApp extends ZIOAppDefault:

  def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    ZIO
      .service[Routes[Any, Response]]
      .flatMap(Server.serve)
      .provide(
        Server.default,
        AppLayer.live
      )
