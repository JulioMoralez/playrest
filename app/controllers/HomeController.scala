package controllers

import actors.PaymentReader.Start
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import javax.inject._
import play.api._
import play.api.mvc._

import java.nio.file.Paths
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

@Singleton
class HomeController @Inject()(@Named("payment-reader") paymentReader: ActorRef, cc: ControllerComponents)
                              (implicit system: ActorSystem, ec: ExecutionContext)
  extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(Nil))
  }

  def upload = Action(parse.multipartFormData) {implicit request =>
      val duration = 5.seconds
      implicit val timeout: Timeout = Timeout(duration.length, duration.unit)
      request.body
      .file("textfile")
      .map { uploadedFile =>
        val path = uploadedFile.ref.path

        val future = paymentReader.ask(Start(path, system)).mapTo[Seq[String]]
        Await.result(future, duration)
        Ok(future.value.get.getOrElse(Nil).mkString("\n"))
//        Ok(views.html.index(Nil))
      }
      .getOrElse {
        Redirect(routes.HomeController.index()).flashing("error" -> "Missing file")
      }
  }
}
