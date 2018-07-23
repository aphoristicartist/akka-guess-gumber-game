package com.aphorin.guessnumber

import akka.actor._
import com.aphorin.guessnumber.GameResolver._

final class Game extends Actor {

  import Game._

  private var players: Set[ActorRef] = Set.empty
  private[aphorin] var randomNumber = scala.util.Random.nextInt(1000)

  def receive: Receive = {
    case JoinGame => {
      players += sender()
      sender() ! Player.Out("Welcome to the game. Let's try to guess the number")
      context.watch(sender())
    }

    case Terminated(user) =>
      players -= user

    case Guess(value) => separate(value, randomNumber) match {
      case response: WinResponse => {
        randomNumber = scala.util.Random.nextInt(1000)
        players.foreach(respond(_, sender(), response))
      }
      case response: UsualResponse => players.foreach(respond(_, sender(), response))
    }
  }
}

object Game {

  private[aphorin] final case object JoinGame

  private[aphorin] final case class Guess(value: Either[String, Int])

  private[aphorin] sealed trait Response {
    val forPlayer: String
    val forAll: String
  }

  private[aphorin] final case class UsualResponse(forPlayer: String, forAll: String) extends Response

  private[aphorin] final case class WinResponse(forPlayer: String, forAll: String) extends Response

}