package controllers.api

import models.User
import play.api.libs.json._
import play.api.mvc._
import services.UserService

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserController @Inject()(cc: ControllerComponents, userService: UserService) extends AbstractController(cc) {

  implicit val todoFormat: OFormat[User] = Json.format[User]

  def getAll: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    userService.getAll.map(items =>
      Ok(Json.toJson(items)))
  }
}
