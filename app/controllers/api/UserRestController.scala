package controllers.api

import models.{User, UserForm}
import play.api.libs.json._
import play.api.mvc._
import services.UserService

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserRestController @Inject()(cc: ControllerComponents, userService: UserService) extends AbstractController(cc) {

  implicit val userFormat: OFormat[User] = Json.format[User]

  def getAll: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userService.getAll.map(users =>
      Ok(Json.toJson(users)))
  }

  def getById(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userService.getUserById(id).map(item =>
      Ok(Json.toJson(item))
    )
  }

  def add(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UserForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("Error!"))
      },
      data => {
        val user = User(0, data.name, data.balance, data.email)
        userService.addUser(user).map( _ => Redirect(routes.UserRestController.getAll()))
      })
  }

  def update(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UserForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("Error!"))
      },
      data => {
        val user = User(id, data.name, data.balance, data.email)
        userService.updateUser(user).map( _ => Redirect(routes.UserRestController.getAll()))
      })
  }

  def delete(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userService.deleteUser(id).map ( _ =>
      Redirect(routes.UserRestController.getAll())
    )
  }




}
