package com.aphorin.guessnumber

import akka.actor._
import com.aphorin.guessnumber.Game.{Response, UsualResponse, WinResponse}

object GameResolver {

  private[aphorin] def respond(targetPlayerRef: ActorRef,
                               currentPlayerRef: ActorRef,
                               response: Response) =
    if (targetPlayerRef.equals(currentPlayerRef))
      targetPlayerRef ! Player.Out(response.forPlayer)
    else
      targetPlayerRef ! Player.Out(response.forAll)

  private[aphorin] def separate(value: Either[String, Int], target: Int): Response = value match {
    case Right(n) => n compare target match {
      case 0 => WinResponse("You win the game! Let's play new game", "Someone win the game. Let's play new game")
      case 1 => UsualResponse("Should be lower", "Someone is trying to solve")
      case -1 => UsualResponse("Should be greater", "Someone is trying to solve")
    }
    case Left(validationMessage) => UsualResponse(validationMessage, "Someone does't understand the rules")
  }

}