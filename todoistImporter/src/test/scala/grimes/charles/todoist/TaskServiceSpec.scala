package grimes.charles.todoist

import cats.effect.IO
import cats.effect.kernel.Resource
import org.http4s.{Header, Headers}
import org.http4s.client.Client
import org.http4s.dsl.io.*
import weaver.IOSuite

import java.nio.charset.StandardCharsets.*
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax

import scala.io.Source

object TaskServiceSpec  extends IOSuite {
  override type Res = String

  override def sharedResource: Resource[IO, String] = Resource.fromAutoCloseable(
    IO.blocking(Source.fromURL(getClass.getResource("/tasks.json"), UTF_8.name))
  ).map(_.mkString)

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val expectedHeaders = Headers(
    Header.Raw(ci"Authorization", "Bearer theapikey"),
    Header.Raw(ci"Accept", "application/json")
  )

  Seq(1, 7).foreach { daysWindow =>
    test(s"Should filter next $daysWindow days of todoist tasks"){ todoistResponse =>
      val clientStub = Client.apply[IO] { request =>
        Resource.eval {
          (
            request.headers == expectedHeaders,
            request.uri.toString == s"https://api.todoist.com/rest/v2/tasks?filter=overdue%7Cnext%20$daysWindow%20days"
          ) match {
            case (false, _) =>
              logger.error(s"Unexpected request headers (${request.headers})") >>
                BadRequest()
            case (true, false) =>
              logger.error(s"Unexpected URL (${request.uri.toString})") >>
                BadRequest()
            case _ => Ok(todoistResponse)
          }
        }
      }

      for {
        result <- TaskService[IO].getIncompletedTasks(
          "theapikey", daysWindow, clientStub
        )
      } yield expect(
        result == List(
          TodoistTask("Install smoke alarm", Due("12/03/2024")),
          TodoistTask("Tidy gym", Due("11/05/2024")),
          TodoistTask("Mow lawn", Due("23/06/2024")),
          TodoistTask("test tomorrow", Due("22/06/2024")),
          TodoistTask("test today", Due("21/06/2024"))
        )
      )
    }
  }
}
