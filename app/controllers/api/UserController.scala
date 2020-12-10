package controllers.api

import models.User
import play.api.libs.json._
import play.api.mvc._
import services.UserService

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Try

class UserController @Inject()(cc: ControllerComponents, userService: UserService) extends AbstractController(cc) {

  implicit val todoFormat: OFormat[User] = Json.format[User]

  def getAll: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userService.getAll.map(users =>
      Ok(Json.toJson(users)))
  }

  def showUsersPost: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val message = args("message").head
      userService.getAll.map(users =>
        Ok(views.html.users(users, message))
      )
    }.getOrElse(userService.getAll.map(users =>
        Ok(views.html.users(users, ""))
    ))
  }

//  def showUsersGet(message: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
//    userService.getAll.map(users =>
//      Ok(views.html.users(users, message))
//    )
//  }

  def deleteUser(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val id = args("id").head.toInt
      userService.deleteUser(id).map ( _ =>
        //Redirect(routes.UserController.showUsersGet(s"Пользователь удален"))
        Ok(views.html.users(Await.result(userService.getAll, 5.seconds), "Пользователь удален"))
      )
    }.getOrElse(Future(Ok(views.html.users(Await.result(userService.getAll, 5.seconds), ""))))
  }

  def editUser(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val user = getUserFromForm(args)
      Ok(views.html.userform(user.get, ""))
    }.getOrElse(Ok(views.html.users(Await.result(userService.getAll, 5.seconds), "")))
  }

  def updateUser(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args: Map[String, Seq[String]] =>
      getUserFromForm(args) match {
        case Some(user) =>
          if (user.balance >= 0) {
            if (user.name.trim.nonEmpty) {
              Await.result(userService.getUserByName(user.name), 5.seconds) match {
                case Some(finduser) =>
                  if (finduser.id == user.id) {
                    userService.updateUser(user).map ( _ =>
                      //Redirect(routes.UserController.showUsersGet(s"${user.name} изменен"))
                      Ok(views.html.users(Await.result(userService.getAll, 5.seconds), s"${user.name} изменен"))
                    )
                  } else {
                    Future(Ok(views.html.userform(user, "Имя занято")))
                  }
                case None =>
                  if (user.id > 0) {
                    userService.updateUser(user).map( _ =>
                      //Redirect(routes.UserController.showUsersGet(s"${user.name} изменен")))
                      Ok(views.html.users(Await.result(userService.getAll, 5.seconds), s"${user.name} изменен")))
                  } else {
                    userService.addUser(user).map( _ =>
                      //Redirect(routes.UserController.showUsersGet(s"${user.name} добавлен")))
                      Ok(views.html.users(Await.result(userService.getAll, 5.seconds), s"${user.name} добавлен")))
                  }
              }
            } else {
              Future(Ok(views.html.userform(user, "Имя не задано")))
            }

          } else {
            Future(Ok(views.html.userform(user, "Баланс задан неверно")))
          }
        case None =>
          userService.getUserById(args("id").head.toLong).map( user =>
            Ok(views.html.userform(user.get, "Ошибка в форме")))

      }
    }.getOrElse(Future(Ok(views.html.users(Await.result(userService.getAll, 5.seconds), ""))))
  }

  def getUserFromForm(args: Map[String, Seq[String]]): Option[User] = {
    Option(User( args("id").head.toLong,
      args("name").head.trim,
      Try(args("balance").head.toInt).getOrElse(-1),
      args("email").head.trim))
  }

  def newUser(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
      Ok(views.html.userform(User(-1, "", 0, ""), ""))
  }
}
