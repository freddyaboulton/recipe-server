package com.freddyaboulton.recipe

import cats.effect.IO
import com.freddyaboulton.recipe.Recipes.{Recipe, RecipeMessage}
import com.freddyaboulton.recipe.database.{Migrations, RecipesRepository}
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import io.circe.syntax._
import org.http4s.circe._

import java.util.UUID


class RecipeSpec extends CatsEffectSuite {

  test("Creating Recipes") {
    val recipeName = s"Recipe - ${UUID.randomUUID()}"
    val createdRecipeResponse = createRecipe(Recipe(name=recipeName))
    val createdRecipe: IO[Recipe] = createdRecipeResponse.as[Recipe]
    assert(createdRecipeResponse.status == Status.Created, clue = s"Expected ${Status.Ok}, got ${createdRecipeResponse.status}")
    assertIO(createdRecipe.map(_.name), recipeName )
    assertIOBoolean(createdRecipe.map(_.id.isDefined), clue="Id was not defined")
  }

  test(name = "retrieving recipes") {
    val recipeName = s"Recipe - ${UUID.randomUUID()}"
    val createdRecipeResponse = createRecipe(Recipe(name=recipeName)).as[Recipe].unsafeRunSync()
    val recipeId = createdRecipeResponse.id.getOrElse(fail("id was not stored"))
    val recipeResponse = getRecipe(recipeId.toString)
    assert(recipeResponse.status == Status.Ok, clue = s"Expected ${Status.Ok}, got ${recipeResponse.status}")
    val recipe = recipeResponse.as[Recipe]
    assertIO(recipe.map(_.name), returns=recipeName)
    assertIOBoolean(recipe.map(_.id.contains(recipeId)), clue="id did not match")
  }

  test(name = "retrieving non-existent recipes") {
    val recipeId = UUID.randomUUID().toString
    val resolvedRecipeResponse = getRecipe(recipeId)
    val resolvedRecipe = resolvedRecipeResponse.as[RecipeMessage]
    assert(resolvedRecipeResponse.status == Status.NotFound, clue= "We should be getting 404")
    assertIO(resolvedRecipe.map(_.message), returns=s"recipe did not exist with following identifier: $recipeId")
  }

  val server: HttpApp[IO] = {
    Migrations.migrate[IO]().compile.drain.unsafeRunSync()
    RecipeserverRoutes.recipeRoutes[IO](new RecipesRepository[IO]()).orNotFound
  }

  private[this] def getRecipe(id: String): Response[IO] = {
    val getRecipe = Request[IO](Method.GET, uri"/recipes" / id)
    this.server.run(getRecipe).unsafeRunSync()
  }

  private[this] def createRecipe(recipe: Recipe): Response[IO] = {
    val postRecipe = Request[IO](Method.POST, uri"/recipes").withEntity(recipe.asJson)
    this.server.run(postRecipe).unsafeRunSync()
  }
}