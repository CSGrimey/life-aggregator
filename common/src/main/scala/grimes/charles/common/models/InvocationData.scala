package grimes.charles.common.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/*
  The contract between the scheduler and the step function 
  who should pass this into each integration lambda
 */
case class InvocationData(daysWindow: Int)

object InvocationData {
  given decoder: Decoder[InvocationData] = deriveDecoder[InvocationData]
  given encoder: Encoder[InvocationData] = deriveEncoder[InvocationData]
}