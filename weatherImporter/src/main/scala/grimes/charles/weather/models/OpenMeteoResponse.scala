package grimes.charles.weather.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class Daily(sunrise: List[String], sunset: List[String])
object Daily {
  given dailyDecoder: Decoder[Daily] = deriveDecoder[Daily]
}

case class Hourly(weather_code: List[Int], temperature_2m: List[Double])
object Hourly {
  given hourlyDecoder: Decoder[Hourly] = deriveDecoder[Hourly]
}

case class OpenMeteoResponse(daily: Daily, hourly: Hourly)
object OpenMeteoResponse {
  given openMeteoResponseDecoder: Decoder[OpenMeteoResponse] = deriveDecoder[OpenMeteoResponse]
}