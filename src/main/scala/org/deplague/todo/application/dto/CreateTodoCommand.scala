package org.deplague.todo.application.dto

case class CreateTodoCommand(title: String, description: Option[String])
