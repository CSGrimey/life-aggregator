package grimes.charles.common.ssm

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class SSMResponse(Parameter: Parameter)

object SSMResponse {
  given decoder: Decoder[SSMResponse] = deriveDecoder[SSMResponse]
}

case class Parameter(Value: String)

object Parameter {
  given decoder: Decoder[Parameter] = deriveDecoder[Parameter]
}