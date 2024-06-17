package grimes.charles

import grimes.charles.common.models.AggregatedData
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class StepFunctionInput(input: AggregatedData)

object StepFunctionInput {
  given decoder: Decoder[StepFunctionInput] = deriveDecoder[StepFunctionInput]
}