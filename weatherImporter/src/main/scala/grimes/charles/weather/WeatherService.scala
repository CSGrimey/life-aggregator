package grimes.charles.weather

import cats.effect.Async
import cats.syntax.all.*
import org.http4s.Method.GET
import org.http4s.Request
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

class WeatherService[F[_] : Async] {
  // https://open-meteo.com/en/docs
  private val openMeteoUrl = uri"https://api.open-meteo.com/v1/forecast"

  def getForecast(daysWindow: Int, client: Client[F])
                 (using logger: Logger[F]): F[List[Weather]] = {
    val request = Request[F](
      method = GET,
      uri = openMeteoUrl
        .withQueryParam("latitude", 51.454265)
        .withQueryParam("longitude", -0.97813)
        .withQueryParam("hourly", "temperature_2m,weather_code")
        .withQueryParam("daily", "sunrise,sunset")
        .withQueryParam("timezone", "Europe/London")
        .withQueryParam("start_date", "2024-10-17")
        .withQueryParam("end_date", "2024-10-17")
        .withQueryParam("models", "ukmo_seamless")
    )

    val forecast = for {
      _ <- logger.info(s"Retrieving weather forecast in Reading for the next $daysWindow days")
      forecast <- client.expect[Weather](request)(jsonOf[F, Weather])
      _ <- logger.info(forecast.toString)
    } yield List(forecast)

    forecast.onError(error =>
      logger.error(s"Failed to retrieve weather forecast. Error = $error")
    )
  }
}