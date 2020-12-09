package controllers

import actors.PaymentReader.Start
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.Files
import play.api.mvc._

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

@Singleton
class HomeController @Inject()(@Named("payment-reader") paymentReader: ActorRef, cc: ControllerComponents)
                              (implicit system: ActorSystem, ec: ExecutionContext)
  extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(Nil))
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
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
