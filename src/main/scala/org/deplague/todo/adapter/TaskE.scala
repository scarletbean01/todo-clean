package org.deplague.todo.adapter

import org.deplague.todo.application.Effect
import org.deplague.todo.domain.DomainError
import zio.ZIO

type TaskE[A] = ZIO[Any, DomainError, A]

given Effect[TaskE] with
  def pure[A](a: A): TaskE[A] = ZIO.succeed(a)
  def map[A, B](fa: TaskE[A])(f: A => B): TaskE[B] = fa.map(f)
  def flatMap[A, B](fa: TaskE[A])(f: A => TaskE[B]): TaskE[B] = fa.flatMap(f)
  def fromEither[A](ea: Either[DomainError, A]): TaskE[A] = ZIO.fromEither(ea)
  def raiseError[A](e: DomainError): TaskE[A] = ZIO.fail(e)
