package services

import com.google.inject.Inject
import models.{User, UserList}

import scala.concurrent.Future

class UserService @Inject() (items: UserList) {
  def getAll: Future[Seq[User]] = {
    items.getAll
  }

  def getUserByName(name: String): Future[Option[User]] = {
    items.getUserByName(name)
  }

  def addUser(user: User): Future[String] = {
    items.addUser(user)
  }

  def updateUser(user: User): Future[Int] = {
    items.updateUser(user)
  }

  def updateUserBalance(name: String, balance: Int): Future[Boolean] = {
    items.updateUserBalance(name, balance)
  }
}
