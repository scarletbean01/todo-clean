package org.deplague.todo.adapter.in.web.request

import zio.json.JsonDecoder

case class CreateTodoRequest(title: String, description: Option[String]) derives JsonDecoder
