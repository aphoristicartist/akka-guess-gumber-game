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
  implicit val executionContext = system.dispatcher

  private val game = system.actorOf(Props[Game])

  val bindingFuture = Http().bindAndHandle(routes(game), "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}