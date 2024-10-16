package grimes.charles.todoist

import cats.effect.Async
import cats.syntax.all.*
import grimes.charles.common.utils.OutputsDate
import org.http4s.Method.*
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.{AuthScheme, Credentials, Header, Headers, Method, Request, Uri}
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import java.time.LocalDate

class TaskService[F[_] : Async] extends OutputsDate {
  // https://developer.todoist.com/rest/v2/#get-active-tasks
  private val todoistV2Url = uri"https://api.todoist.com/rest/v2/tasks"

  def getDueTasks(apiKey: String, daysWindow: Int, client: Client[F])
                 (using logger: Logger[F]): F[List[TodoistTask]] = {
    val dayAhead = daysWindow + 1
    val request = Request[F](
      method = GET,
      uri = todoistV2Url.withQueryParam("filter", s"overdue|next $daysWindow days"),
      headers = Headers(
        Authorization(Credentials.Token(AuthScheme.Bearer, apiKey))
      )
    )

    val todoistTasks = for {
      _ <- logger.info("Retrieving due tasks")
      tasks <- client.expect[List[TodoistTask]](request)(jsonOf[F, List[TodoistTask]])
      formattedTasks = tasks.map { task =>
        task.copy(due = Due(LocalDate.parse(task.due.date).format(dateFormatter)))
      }.sortBy(task => LocalDate.parse(task.due.date, dateFormatter).toEpochDay)
    } yield formattedTasks

    todoistTasks.onError(error =>
      logger.error(s"Failed to retrieve todoist tasks. Error = $error")
    )
  }
}