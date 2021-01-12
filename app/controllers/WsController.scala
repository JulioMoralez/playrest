package controllers

import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WsController @Inject() (ws: WSClient, cc: ControllerComponents)extends AbstractController(cc){

    // пробовал компонент WSClient, просто выводит свой Ip адрес
    def getIp: Action[AnyContent] = Action.async { implicit request =>
      ws
        .url("https://ifconfig.me/")
        .withHttpHeaders(USER_AGENT -> "curl")
        .get()
        .map(x => Ok(x.body))
    }
}
