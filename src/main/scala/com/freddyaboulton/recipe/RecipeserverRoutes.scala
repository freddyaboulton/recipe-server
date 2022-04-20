package com.freddyaboulton.recipe

import cats.effect.Concurrent
import com.freddyaboulton.recipe.Recipes.{Recipe, RecipeMessage}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.implicits._

object RecipeserverRoutes {

  def recipeRoutes[F[_]: Concurrent](recipes: Recipes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case request @ POST -> Root / "recipes" =>
        for {
          recipe <- request.as[Recipe]
          createdRecipeE <- recipes.create(recipe)
          resp <- createdRecipeE match {
            case Left(message) => BadRequest(message)
            case Right(created) => Created(created)
          }
        } yield resp
      case GET -> Root / "recipes" / id =>
        for {
          maybeRecipe <- recipes.findById(id)
          resp <- maybeRecipe match {
            case Right(recipe) => Ok(recipe)
            case Left(message @ RecipeMessage(m)) if m.startsWith("recipe did not exist with following") => NotFound(message)
            case Left(message) => BadRequest(message)
          }
        } yield resp
    }
  }
}