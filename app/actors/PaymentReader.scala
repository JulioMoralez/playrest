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
import scala.util.Try

object PaymentReader extends Serializable {
  final case class Start(path: Path, system: ActorSystem)

  sealed trait Transaction
  final case class GoodTransaction(from: String, to: String, value: Int) extends Transaction
  final case class BadTransaction(transaction: String) extends Transaction

  val defaultUserBalance = 100
  val users: mutable.Map[String, Int] = mutable.Map()



  def checkTransaction(transaction: String): Transaction = {

    val paymentRegex = "([A-Za-z0-9]+) (->) ([A-Za-z0-9]+) (:) ([0-9]+)"
//    val paymentRegex = "[A-Za-z0-9]+ -> [A-Za-z0-9]+ : [0-9]+"
    val regex = paymentRegex.r()
    println(transaction)
//    val option: Option[String] = regex.findFirstIn(transaction)
//    option match {
//      case Some(s) => println(s"Some($s)")
//      case None => println("None")
//    }

    regex.findFirstMatchIn(transaction)
      .map { r =>
        println(r.group(2))
        GoodTransaction(r.group(1), r.group(3), r.group(5).toInt)
      }.getOrElse(BadTransaction(transaction))



//    transaction match {
//      case regex(from, _, to, _, value) =>
//        GoodTransaction(from, to, value.toInt)
//      case _ =>
//        BadTransaction(transaction)
//    }


    // не работает на heroku regex!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

//    val regex = "[A-Za-z0-9]+"
//    val fromIndex = transaction.indexOf("->")
//    if (fromIndex > 0) {
//      val from = transaction.substring(0, fromIndex).trim
//      if (from.matches(regex)) {
//        val s1 = transaction.substring(fromIndex + 2, transaction.length).trim
//        val toIndex = s1.indexOf(":")
//        if (toIndex > 0) {
//          val to = s1.substring(0, toIndex).trim
//          if (to.matches(regex)) {
//            val s2 = s1.substring(toIndex + 1, s1.length).trim
//            val value = Try(s2.toInt).getOrElse(-1)
//            if (value > 0) {
//              return GoodTransaction(from, to, value)
//            }
//          }
//        }
//      }
//    }
//    BadTransaction(transaction)
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
          s"$from -> $to : $value Успех. Новый баланс: $from=$newBalanceFrom, $to=$newBalanceTo"
        } else {
          s"$from -> $to : $value Отмена операции"
        }
      case BadTransaction(transaction) =>
        s"[Ошибка] $transaction"
    }
  }

  def receive: Receive = {
    case Start(path, system) =>
      println("start")
      try {
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
      } catch {
        case ex: Exception =>
          users.clear()
          sender() ! Nil
      }
  }
}


