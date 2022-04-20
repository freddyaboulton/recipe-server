package com.freddyaboulton.recipe

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import com.freddyaboulton.recipe.database.{Migrations, RecipesRepository}
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

object RecipeserverServer {

  def stream[F[_]: Async]: Stream[F, Nothing] = {

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract a segments not checked
    // in the underlying routes.
    val httpApp = (
      RecipeserverRoutes.recipeRoutes[F](new RecipesRepository[F]())
      ).orNotFound

    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)
    for {
      // don't start server if migrations fail
      _ <- Migrations.migrate[F]()
      exitCode <- Stream.resource(
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"3000")
          .withHttpApp(finalHttpApp)
          .build >>
        Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
