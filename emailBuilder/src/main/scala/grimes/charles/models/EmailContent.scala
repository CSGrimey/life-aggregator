package grimes.charles.models

import io.circe.generic.semiauto.deriveEncoder
import io.circe.Encoder

case class EmailContent(subject: String, body: String)

object EmailContent {
  given encoder: Encoder[EmailContent] = deriveEncoder[EmailContent]
}