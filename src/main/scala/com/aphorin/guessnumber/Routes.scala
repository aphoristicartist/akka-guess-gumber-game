package com.aphorin.guessnumber

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{get, handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

trait Routes {
  val system: ActorSystem

  def routes(gameRef: ActorRef): Route =
    path("guessgame") {
      get {
        handleWebSocketMessages(playerFlow(gameRef))
      }
    }

  def playerFlow(gameRef: ActorRef): Flow[Message, Message, NotUsed] = {

    val player = system.actorOf(Props(new Player(gameRef)))

    val sink: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) => Player.In(text)
      }.to(Sink.actorRef[Player.In](player, PoisonPill))

    val source: Source[Message, NotUsed] =
      Source.actorRef[Player.Out](20, OverflowStrategy.fail)
        .mapMaterializedValue { outgoing =>
          player ! Player.Connected(outgoing)
          NotUsed
        }.map(out => TextMessage(out.message))

    Flow.fromSinkAndSource(sink, source)
  }

}
