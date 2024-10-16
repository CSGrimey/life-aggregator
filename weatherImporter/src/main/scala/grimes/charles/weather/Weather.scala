package grimes.charles.weather

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

case class Weather(daily: Daily, hourly: Hourly)
object Weather {
  given weatherDecoder: Decoder[Weather] = deriveDecoder[Weather]
}