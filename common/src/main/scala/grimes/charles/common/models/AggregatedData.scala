package grimes.charles.common.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/*
  The contract between integration lambdas and the email builder lambda
 */
case class AggregatedData(daysWindow: Int, aggregationType: String, aggregationResults: List[String])

object AggregatedData {
  given decoder: Decoder[AggregatedData] = deriveDecoder[AggregatedData]
  given encoder: Encoder[AggregatedData] = deriveEncoder[AggregatedData]
}