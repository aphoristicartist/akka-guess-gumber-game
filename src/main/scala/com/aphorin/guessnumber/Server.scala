package com.aphorin.guessnumber

import akka.actor._
import akka.http.scaladsl._
import akka.stream._
import scala.io.StdIn
import scala.concurrent.ExecutionContext
import scala.util._

object Server extends App with Routes {

    implicit val system = ActorSystem("GameActorSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContent = system.dispatcher

    private val game = system.actorOf(Props(new Game), "guessgame")

    Http().bindAndHandle(routes(game), "127.0.0.1", 8080).onComplete {
      case Success(binding) =>
        println(s"Server online at http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}\n press Enter to kill server")

      case Failure(ex) =>
        println(s"Failed to start server, shutting down actor system. Exception is: ${ex.getCause}: ${ex.getMessage}")
        system.terminate()
    }
    StdIn.readLine()
    system.terminate()

}