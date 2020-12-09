package actors

import actors.PaymentReader.{BadTransaction, GoodTransaction, Start, Transaction, checkTransaction, defaultUserBalance, users}
import akka.actor.{Actor, ActorSystem}
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.{ByteString, Timeout}
import models.User
import services.UserService

import java.nio.file.Path
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import javax.inject._

object PaymentReader extends Serializable {
  final case class Start(path: Path, system: ActorSystem)

  sealed trait Transaction
  final case class GoodTransaction(from: String, to: String, value: Int) extends Transaction
  final case class BadTransaction(transaction: String) extends Transaction

  val defaultUserBalance = 100
  val users: mutable.Map[String, Int] = mutable.Map()



  def checkTransaction(transaction: String): Transaction = {
    val paymentRegex = "([A-Za-z0-9]+) (->) ([A-Za-z0-9]+) (:) ([0-9]+)"
    val regex = paymentRegex.r()
    println("check " + regex)
    transaction match {
      case regex(from, _, to, _, value) =>
        println("good")
        GoodTransaction(from, to, value.toInt)
      case _ =>
        println("bad")
//        BadTransaction(transaction)
        GoodTransaction("Max", "Bob", 1)
    }
  }
}

class PaymentReader @Inject() (userService: UserService) extends Actor with Serializable {

  def checkNewUsers(names: Vector[String]): Unit = {
    names.foreach(name =>
      if (!users.contains(name)) {
        val user = userService.getUserByName(name)
        Await.result(user, 5.seconds)
        val startBalance = user.value.get.get match {
          case Some(user) => user.balance //getOrElse(defaultUserBalance)
          case None       =>
            userService.addUser(User(-1, name, defaultUserBalance, s"$name@mail"))
            defaultUserBalance
        }
        users.put(name, startBalance)
      }
    )
  }

  def process(transaction: Transaction): String = {
    transaction match {
      case GoodTransaction(from, to, value) =>
        checkNewUsers(Vector(from, to))

        val newBalanceFrom = users(from) - value
        if (newBalanceFrom >= 0) {
          val newBalanceTo = users(to) + value
          users(from) = newBalanceFrom
          users(to) = newBalanceTo
          s"$from $to $value Payment successful. New balance: $from=$newBalanceFrom, $to=$newBalanceTo"
        } else {
          s"$from $to $value Canceling a payment"
        }
      case BadTransaction(transaction) =>
        s"not ok $transaction"
    }
  }

  def receive: Receive = {
    case Start(path, system) =>
      println("start")
      val source = FileIO
        .fromPath(path)
        .via(Framing.delimiter(ByteString(System.lineSeparator()), 128, allowTruncation = true)
          .map(_.utf8String)).filter(_.nonEmpty)

      implicit val materializer: ActorSystem = system
      implicit val timeout: Timeout = 5.seconds

      val future = source
        .map(checkTransaction)
        .map(process)
        .runWith(Sink.seq[String])
      Await.result(future, 5.seconds)
      users.foreach(user => userService.updateUserBalance(user._1, user._2))
      users.clear()
      sender() ! future.value.get.getOrElse(Nil)
  }
}


