package grimes.charles.todoist

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class TodoistTask(content: String, due: Due)
case class Due(date: String)

object TodoistTask {
  given decoder: Decoder[TodoistTask] = deriveDecoder[TodoistTask]
}