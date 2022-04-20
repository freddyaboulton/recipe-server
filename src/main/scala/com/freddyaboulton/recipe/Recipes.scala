package com.freddyaboulton.recipe

import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import io.circe.generic.semiauto._
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder}
import com.freddyaboulton.recipe.Recipes._

import java.util.UUID
import scala.collection.mutable.ListBuffer

trait Recipes[F[_]] {
  def create(recipe: Recipes.Recipe): F[Either[RecipeMessage, Recipe]]
  def findById(id: String): F[Either[RecipeMessage, Recipe]]
}


object Recipes {

  def impl[F[_] : Concurrent] : Recipes[F] = new Recipes[F] {

    val recipes = new ListBuffer[Recipe]()

    override def create(recipe: Recipe): F[Either[RecipeMessage, Recipe]] = {
      val createdRecipe = recipe.copy(id=Some(UUID.randomUUID()))
      recipes += createdRecipe
      Concurrent[F].pure(Right(createdRecipe))
    }

    private def resolveId(id: String): Option[UUID] = {
      try {
        Option(UUID.fromString(id))
      } catch {
        case _: IllegalArgumentException => None
      }
    }

    override def findById(id: String): F[Either[RecipeMessage, Recipe]] = {
      resolveId(id) match {
        case Some(uuid) => Concurrent[F].pure {
          recipes.find(_.id.contains(uuid)) match {
            case Some(recipe) => Right(recipe)
            case None => Left(RecipeMessage(s"recipe did not exist with following identifier: $id"))
          }
        }
        case None => Concurrent[F].pure(Left(RecipeMessage(s"provided identifier was invalid: $id")))
      }
    }

  }

  final case class Recipe(id: Option[UUID] = None, name: String)
  final case class RecipeMessage(message: String)

  object Recipe {
    implicit val recipeDecoder: Decoder[Recipe] = deriveDecoder[Recipe]
    implicit def recipeEntityDecoder[F[_]: Concurrent] : EntityDecoder[F, Recipe] = jsonOf
    implicit val recipeEncoder: Encoder[Recipe] = deriveEncoder[Recipe]
    implicit def recipeEntityEncoder[F[_]] : EntityEncoder[F, Recipe] = jsonEncoderOf
  }

  object RecipeMessage {
    implicit val recipeMessageDecoder: Decoder[RecipeMessage] = deriveDecoder[RecipeMessage]
    implicit def recipeMessageEntityDecoder[F[_]: Concurrent] : EntityDecoder[F, RecipeMessage] = jsonOf
    implicit val recipeMessageEncoder: Encoder[RecipeMessage] = deriveEncoder[RecipeMessage]
    implicit def recipeMessageEntityEncoder[F[_]] : EntityEncoder[F, RecipeMessage] = jsonEncoderOf
  }

}
