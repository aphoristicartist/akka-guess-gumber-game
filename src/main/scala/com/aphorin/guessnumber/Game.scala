package com.aphorin.guessnumber

import akka.actor._

object Game {

  private[aphorin] case object JoinGame

  private[aphorin] final case class Guess(value: Either[String, Int])

  private[aphorin] sealed abstract class Response {
    val forPlayer: String
    val forAll: String
  }
  private[aphorin] final case class UsualResponse(forPlayer: String, forAll: String) extends Response
  private[aphorin] final case class WinResponse(forPlayer: String, forAll: String) extends Response

}

final class Game extends Actor {

  import Game._

  private var players: Set[ActorRef] = Set.empty
  private var randomNumber = scala.util.Random.nextInt(1000)

  private def respond(ref: ActorRef, response: Response) =
    if (ref.equals(sender()))
      ref ! Player.Out(response.forPlayer)
    else
      ref ! Player.Out(response.forAll)

  private def separate(value: Either[String, Int]) = value match {
    case Right(n) => n compare randomNumber match {
      case 0 => WinResponse("You win the game! Let's play new game", "Someone win the game. Let's play new game")
      case 1 => UsualResponse("Should be lower", "Someone trying to solve")
      case -1 => UsualResponse("Should be greater", "Someone trying to solve")
    }
    case Left(validationMessage) => UsualResponse(validationMessage, "Someone does't understand the rules")
  }

  def receive = {
    case JoinGame => {
      players += sender()
      sender() ! Player.Out("Welcome to the game. Let's try to guess the number")
      context.watch(sender())
    }

    case Terminated(user) =>
      players -= user

    case Guess(value) => separate(value) match {
        case response: WinResponse => {
          randomNumber = scala.util.Random.nextInt(1000)
          players.foreach(ref => respond(ref, response))
        }
        case response: UsualResponse => players.foreach(ref => respond(ref, response))
    }
  }
}