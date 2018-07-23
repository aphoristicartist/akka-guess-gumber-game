package com.aphorin.guessnumber

import akka.actor.{ActorRef, LoggingFSM, Terminated}
import com.aphorin.guessnumber.Game._
import com.aphorin.guessnumber.GameResolver._

sealed trait GameState

case object GameLoop extends GameState

class GameFSM extends LoggingFSM[GameState, GameData] {

  startWith(GameLoop, GameData.initial)

  when(GameLoop) {
    case Event(JoinGame, _) => stay using (stateData.addNewPlayer(sender()))
    case Event(Terminated, _) => stay using (stateData.removePlayer(sender()))
    case Event(Guess(value), data) => separate(value, data.target) match {
      case response: WinResponse => {
        data.players.foreach(respond(_, sender(), response))
        stay using (data.generateNewTarget)
      }
      case response: UsualResponse => {
        data.players.foreach(respond(_, sender(), response))
        stay()
      }
    }
    case _ => throw new NotImplementedError()
  }

  initialize()
}

case class GameData(target: Int, players: Set[ActorRef]) {
  def addNewPlayer(player: ActorRef) = GameData(target, players + player)

  def removePlayer(player: ActorRef) = GameData(target, players - player)

  def generateNewTarget = GameData(scala.util.Random.nextInt(1000), players)
}

object GameData {
  def initial = GameData(scala.util.Random.nextInt(1000), Set.empty)
}