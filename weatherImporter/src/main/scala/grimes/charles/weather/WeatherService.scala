package grimes.charles.weather

import cats.effect.Async
import cats.syntax.all.*
import grimes.charles.common.utils.DateUtils
import org.http4s.Method.GET
import org.http4s.Request
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*
import java.time.Instant
import java.util.Date

class WeatherService[F[_] : Async] extends DateUtils {
  // https://open-meteo.com/en/docs
  private val openMeteoUrl = uri"https://api.open-meteo.com/v1/forecast"

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  def getForecast(now: Instant, daysWindow: Int, client: Client[F])
                 (using logger: Logger[F]): F[List[Weather]] = {
    val startDate = toStartOfNextDay(now)
    val endDate = Date.from(startDate.toInstant.plus(daysWindow - 1, DAYS))

    val request = Request[F](
      method = GET,
      uri = openMeteoUrl
        .withQueryParam("latitude", 51.454265)
        .withQueryParam("longitude", -0.97813)
        .withQueryParam("hourly", "temperature_2m,weather_code")
        .withQueryParam("daily", "sunrise,sunset")
        .withQueryParam("timezone", timeZone)
        .withQueryParam("start_date", dateFormat.format(startDate))
        .withQueryParam("end_date", dateFormat.format(endDate))
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