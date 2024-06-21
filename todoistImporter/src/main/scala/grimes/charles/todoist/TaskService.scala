package grimes.charles.todoist

import cats.effect.Async
import cats.syntax.all.*
import org.http4s.Method.*
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.{AuthScheme, Credentials, Header, Headers, Method, Request, Uri}
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

class TaskService[F[_] : Async] {
  // https://developer.todoist.com/rest/v2/#get-active-tasks
  private val todoistV2Url = uri"https://api.todoist.com/rest/v2/tasks"

  def getIncompletedTasks(apiKey: String, daysWindow: Int, client: Client[F])
                         (using logger: Logger[F]): F[List[TodoistTask]] = {
    val request = Request[F](
      method = GET,
      uri = todoistV2Url
        .withQueryParam("filter", s"overdue|next $daysWindow days"),
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, apiKey))
      )
    )

    val todoistTasks = for {
      _ <- logger.info(s"Retrieving incompleted tasks due before the next $daysWindow days")
      tasks <- client.expect[List[TodoistTask]](request)(jsonOf[F, List[TodoistTask]])
    } yield tasks

    todoistTasks.onError(error =>
      logger.error(s"Failed to retrieve todoist tasks. Error = $error")
    )
  }
}