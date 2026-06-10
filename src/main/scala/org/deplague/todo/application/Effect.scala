package org.deplague.todo.application

import org.deplague.todo.domain.DomainError

/** Minimal effect typeclass for tagless final in the application layer.
  *
  * Domain remains pure (Either). The application layer lifts domain results
  * into an abstract effect F[_]. Adapters provide the concrete implementation
  * (e.g., ZIO, synchronous Either for tests).
  */
trait Effect[F[_]]:
  def pure[A](a: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def fromEither[A](ea: Either[DomainError, A]): F[A]
  def raiseError[A](e: DomainError): F[A]

object Effect:
  def apply[F[_]: {Effect as F}]: Effect[F] = F

  def pure[F[_]: Effect, A](a: A): F[A] = apply[F].pure(a)

  def fromEither[F[_]: Effect, A](ea: Either[DomainError, A]): F[A] =
    apply[F].fromEither(ea)

  def raiseError[F[_]: Effect, A](e: DomainError): F[A] =
    apply[F].raiseError(e)

  // Extension methods enable `for` comprehensions on abstract F[A].
  // When F is concrete (e.g., ZIO, Either), instance methods take precedence.
  extension [F[_], A](fa: F[A])(using ev: Effect[F])
    def map[B](f: A => B): F[B] = ev.map(fa)(f)
    def flatMap[B](f: A => F[B]): F[B] = ev.flatMap(fa)(f)
