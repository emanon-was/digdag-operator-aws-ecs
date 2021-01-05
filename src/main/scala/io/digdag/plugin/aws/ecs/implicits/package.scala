package io.digdag.plugin.aws.ecs

import scala.util.{Try, Success, Failure}
import scala.reflect.ClassTag
import io.digdag.client.config.Config
import io.circe.{Json, JsonObject}
import org.atnos.eff._

package object implicits {

  trait Err {
    def panic = throw exception
    def exception: Throwable = new Exception("${this}")
    def cause: Throwable
  }

  object Err {
    trait Throws extends Err {
      val err: Throwable
      def cause = err
    }
    trait Def extends Err {
      val err: Any
      def cause = new Exception("${this}")
    }
    trait Enum extends Err {
      val err: Err
      def cause = err.cause
    }
  }

  implicit class KotlinScopeFunctions[T](val self: T) {
    def let[U](f: T => U): U = f(self)
    def also[U](f: T => U): T = { f(self); self }
  }

  implicit class Builder[T](val self: T) {
    def required[A](a: A)(f: (T, A) => T): T = f(self, a)
    def optional[A](a: Option[A])(f: (T, A) => T): T = a match {
      case None => self
      case Some(a) => f(self, a)
    }
  }

  implicit class JsonCascade(val self: Json) {
    def cascade(keys: String*): Try[Json] = Try {
      val emptyJson = Json.fromJsonObject(JsonObject.empty)
      val jsons = keys.map(self \\ _).flatten :+ self
      jsons.fold(emptyJson)((a, b) => a.deepMerge(b))
    }
  }

  implicit class OptionToEither[T](val self: Option[T]) {
    def toEither(): Either[Unit, T] = self match {
      case Some(a) => Right(a)
      case None => Left(())
    }
  }

  implicit class TryToEither[T](val self: Try[T]) {
    def toEither(): Either[Throwable, T] = self match {
      case Failure(a) => Left(a)
      case Success(b) => Right(b)
    }
    def toEither[E](f: Throwable => E): Either[E, T] = self match {
      case Failure(a) => Left(f(a))
      case Success(b) => Right(b)
    }
  }
}
