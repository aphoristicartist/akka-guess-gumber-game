package com.aphorin.guessnumber

import akka.actor.{Actor, ActorRef}

object TryParse {

  private[aphorin] final class StringToEither(val s: String) {
    def checked: Either[String, Int] = {
      try {
        Right(s.toInt)
      } catch {
        case _: Throwable => Left("Wrong format. Only integers accepted")
      }
    }
  }

  implicit def stringToEither(s: String) = new StringToEither(s)
}

final class Player(game: ActorRef) extends Actor {

  import Player._
  import TryParse._

  def receive: Receive = {
    case Connected(outgoing) =>
      context.become(connected(outgoing))
      game ! Game.JoinGame
  }

  private def connected(outgoing: ActorRef): Receive = {
    case In(text) => game ! Game.Guess(text.checked)

    case msg: Out => outgoing ! msg
  }

}

object Player {

  private[aphorin] final case class Connected(outgoing: ActorRef)

  private[aphorin] final case class In(message: String)

  private[aphorin] final case class Out(message: String)

}
