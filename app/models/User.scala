package models

import com.google.inject.Inject
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._

case class User (id: Long, name: String, balance: Int, email: String)

case class UserFormData(name: String, balance: Int, email: String)

object UserForm {
  val form = Form(
    mapping(
      "name" -> nonEmptyText,
      "balance" -> number,
      "email" -> nonEmptyText
    )(UserFormData.apply)(UserFormData.unapply)
  )
}

class UserTableDef(tag: Tag) extends Table[User](tag, "t_users2") {

  def id = column[Long]("user_id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("user_name")
  def balance = column[Int]("user_balance")
  def email = column[String]("user_email")

  override def * = (id, name, balance, email) <> (User.tupled, User.unapply)
}

class UserList @Inject()(
                          protected val dbConfigProvider: DatabaseConfigProvider
                        )(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  var userList = TableQuery[UserTableDef]

  def getAll: Future[Seq[User]] = {
    dbConfig.db.run(userList.result)
  }

  def getUserByName(name: String): Future[Option[User]] = {
    dbConfig.db.run(userList.filter(_.name === name).result.headOption)
  }

  def getUserById(id: Long): Future[Option[User]] = {
    dbConfig.db.run(userList.filter(_.id === id).result.headOption)
  }

  def addUser(user: User): Future[String] = {
    dbConfig.db
      .run(userList += user)
      .map(_ => "user successfully added")
      .recover {
        case ex: Exception => {
          println(ex.getMessage)
          ex.getMessage
        }
      }
  }

  def updateUser(user: User): Future[Int] = {
    dbConfig.db
      .run(userList.filter(_.id === user.id)
        .map(x => (x.name, x.balance, x.email))
        .update(user.name, user.balance, user.email)
      )
  }

  def updateUserBalance(name: String, balance: Int): Future[Boolean] = {
    val query = for (user <- userList if user.name === name)
      yield user.balance
    db.run(query.update(balance)).map(_ > 0)
  }

  def deleteUser(id: Long): Future[Int] = {
    dbConfig.db.run(userList.filter(_.id === id).delete)
  }
}

