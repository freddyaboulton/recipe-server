package com.freddyaboulton.recipe.database

import cats.effect.Sync
import com.freddyaboulton.recipe.Recipes
import com.freddyaboulton.recipe.Recipes.{Recipe, RecipeMessage}

import java.util.UUID

class RecipesRepository[F[_]: Sync] extends Recipes[F] {

  import ctx._

  val recipes = quote {
    querySchema[Recipe](entity = "recipes")
  }

  override def create(recipe: Recipe): F[Either[Recipes.RecipeMessage, Recipes.Recipe]] = {
    Sync[F].delay {
      ctx.run(recipes.insert(_.name -> lift(recipe.name)).returning(_.id)).map {
        uuid => Right(recipe.copy(id = Some(uuid)))
      }.getOrElse(Left(RecipeMessage("")))
    }
  }

  override def findById(id: String): F[Either[Recipes.RecipeMessage, Recipes.Recipe]] = {
    val uuidId = UUID.fromString(id)

    Sync[F].delay {
      ctx.run(recipes.filter(_.id.contains(lift(uuidId)))) match {
        case Seq(recipe) => Right(recipe)
        case Seq() => Left(RecipeMessage(s"recipe did not exist with following identifier: $uuidId"))
        case _ => Left(RecipeMessage(""))
      }
    }
  }


}
