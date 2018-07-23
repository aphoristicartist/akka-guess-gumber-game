package com.aphorin.guessnumber

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import com.aphorin.guessnumber.Player.Connected

class GameSpec extends TestKit(ActorSystem("GameSpecActorSystem"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  trait PlayerScope {
    val gameProbe = TestProbe()
    val outgoing = TestProbe()
    val playerRef = system.actorOf(Props(classOf[Player], gameProbe.ref))

    def playerJoin(): Unit = {
      playerRef ! Connected(outgoing.ref)
      gameProbe.expectMsg(Game.JoinGame)
    }

  }

  "An Player actor" must {
    "send game join message to the game actor" in new PlayerScope {
      playerRef ! Connected(outgoing.ref)
      gameProbe.expectMsg(Game.JoinGame)
    }

    "send guessed and parsed value to the game actor" in new PlayerScope {
      playerJoin()
      playerRef ! Player.In("10")
      gameProbe.expectMsg(Game.Guess(Right(10)))
    }

    "send error message from when parsing failed" in new PlayerScope {
      playerJoin()
      playerRef ! Player.In("text")
      gameProbe.expectMsg(Game.Guess(Left("Wrong format. Only integers accepted")))
    }

    "send forward message to outgoing" in new PlayerScope {
      playerJoin()
      playerRef ! Player.Out("message to outgoing")
      gameProbe.expectNoMessage()
      outgoing.expectMsg(Player.Out("message to outgoing"))
    }
  }

  trait GameScope {
    val playerProbe = TestProbe()
    val secondPlayerProbe = TestProbe()
    val gameRef = TestActorRef[Game]

    def firstJoin(): Unit = {
      playerProbe.send(gameRef, Game.JoinGame)
      playerProbe.expectMsg(Player.Out("Welcome to the game. Let's try to guess the number"))
    }

    def secondJoin(): Unit = {
      secondPlayerProbe.send(gameRef, Game.JoinGame)
      secondPlayerProbe.expectMsg(Player.Out("Welcome to the game. Let's try to guess the number"))
    }
  }

  "An Game actor" must {

    "send response about joining to the player" in new GameScope {
      playerProbe.send(gameRef, Game.JoinGame)
      playerProbe.expectMsg(Player.Out("Welcome to the game. Let's try to guess the number"))
      secondPlayerProbe.send(gameRef, Game.JoinGame)
      playerProbe.expectNoMessage()
      secondPlayerProbe.expectMsg(Player.Out("Welcome to the game. Let's try to guess the number"))
    }

    "send should be greater response" in new GameScope {
      firstJoin()
      secondJoin()
      playerProbe.send(gameRef, Game.Guess(Right(-100)))
      playerProbe.expectMsg(Player.Out("Should be greater"))
      secondPlayerProbe.expectMsg(Player.Out("Someone is trying to solve"))
    }

    "send should be lower response" in new GameScope {
      firstJoin()
      secondJoin()
      playerProbe.send(gameRef, Game.Guess(Right(1001)))
      playerProbe.expectMsg(Player.Out("Should be lower"))
      secondPlayerProbe.expectMsg(Player.Out("Someone is trying to solve"))
    }

    "send wrong format message" in new GameScope {
      firstJoin()
      secondJoin()
      playerProbe.send(gameRef, Game.Guess(Left("Wrong format. Only integers accepted")))
      playerProbe.expectMsg(Player.Out("Wrong format. Only integers accepted"))
      secondPlayerProbe.expectMsg(Player.Out("Someone does't understand the rules"))
    }

    "send win game message" in new GameScope {
      firstJoin()
      secondJoin()
      playerProbe.send(gameRef, Game.Guess(Right(gameRef.underlyingActor.randomNumber)))
      playerProbe.expectMsg(Player.Out("You win the game! Let's play new game"))
      secondPlayerProbe.expectMsg(Player.Out("Someone win the game. Let's play new game"))
    }

  }

}
