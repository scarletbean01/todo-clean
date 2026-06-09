package org.deplague.todo.domain

import zio.test.*

object TitleSpec extends ZIOSpecDefault:
  def spec = suite("Title")(
    test("should reject empty title") {
      assertTrue(Title.create("") == Left(DomainError.ValidationError("Title must be non-empty")))
    },
    test("should reject whitespace-only title") {
      assertTrue(Title.create("   ") == Left(DomainError.ValidationError("Title must be non-empty")))
    },
    test("should reject title > 100 chars") {
      assertTrue(
        Title.create("a" * 101) == Left(DomainError.ValidationError("Title must be at most 100 characters"))
      )
    },
    test("should accept valid title") {
      assertTrue(Title.create("Valid Title").isRight)
    }
  )
