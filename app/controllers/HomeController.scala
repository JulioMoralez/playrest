package controllers

import actors.PaymentReader.Start
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.Files
import play.api.mvc._

import java.nio.file.Paths
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

@Singleton
class HomeController @Inject()(@Named("payment-reader") paymentReader: ActorRef, cc: ControllerComponents)
                              (implicit system: ActorSystem, ec: ExecutionContext)
  extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(Nil, "", ""))
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
      val duration = 5.seconds
      implicit val timeout: Timeout = Timeout(duration.length, duration.unit)
      println("111111111111111111111111111111111")
      request.body
      .file("textfile")
      .map { uploadedFile =>
        println("222222222222222222222222222222222222222")
        val filename    = Paths.get(uploadedFile.filename).getFileName
        val path = uploadedFile.ref.path
        println("3333333333333333333333333333333333333333")
        val future = paymentReader.ask(Start(path, system)).mapTo[Seq[String]]
        Await.result(future, duration)
        val results = future.value.get.getOrElse(Nil)
        if (results.nonEmpty) {
          Ok(views.html.index(results, s"Обработано ${results.length} строк, файл $filename", ""))
        } else {
          Ok(views.html.index(results, "", s"Ошибка $filename"))
        }
      }
      .getOrElse {
        //Redirect(routes.HomeController.index())//.flashing("error" -> "Missing file")
        Ok(views.html.index(Nil, "", s"Файл не выбран"))
      }
  }
}
