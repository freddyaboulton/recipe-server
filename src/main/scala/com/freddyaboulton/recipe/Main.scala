package com.freddyaboulton.recipe

import cats.effect.{ExitCode, IO, IOApp}

// cats-effect
object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    RecipeserverServer.stream[IO].compile.drain.as(ExitCode.Success)
}
