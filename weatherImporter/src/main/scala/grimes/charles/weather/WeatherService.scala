package grimes.charles.weather

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all.*
import grimes.charles.common.utils.DateUtils
import grimes.charles.weather.models.{DayForecast, HourForecast, OpenMeteoResponse, WmoCodes}
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

  private val startHour = 9 // Start daily forecast at 08:00
  private val dayForecastWindow = 24 - startHour  // Only grab remaining hours of data for the day from 08:00

  private val default = "Unknown"

  private def extractDailyForecast(startDate: Date, daysWindow: Int, openMeteoResponse: OpenMeteoResponse): F[NonEmptyList[DayForecast]] = {
    import openMeteoResponse._

    // Todo: Needs major refactoring before deploying
    val dailyForecast = (0 until daysWindow).map(day => {
      val sunrise = daily.sunrise.get(daysWindow).getOrElse(default)
      val sunset = daily.sunset.get(daysWindow).getOrElse(default)

      val hourlyForecast = (0 until dayForecastWindow).map(hour => {
        val hourIndex = (startHour * daysWindow) + hour

        val temperature = hourly.temperature_2m.get(hourIndex).map(t => s"$t°C").getOrElse(default)
        val weather = hourly.weather_code.get(hourIndex).flatMap(WmoCodes.mapping.get).getOrElse(default)

        HourForecast(temperature, hour, weather)
      }).toList

      // Todo: Make this safe
      DayForecast(Date.from(startDate.toInstant.plus(day, DAYS)), sunrise, sunset, hourlyForecast = NonEmptyList.fromListUnsafe(hourlyForecast))
    }).toList

    Async[F].fromOption(NonEmptyList.fromList(dailyForecast), RuntimeException("Failed to extract daily forecast"))
  }

  def getForecast(now: Instant, daysWindow: Int, client: Client[F])
                 (using logger: Logger[F]): F[NonEmptyList[DayForecast]] = {
    val forecast = for {
      _ <- logger.info(s"Retrieving weather forecast in Reading for the next $daysWindow days")
      startDate = toStartOfNextDay(now)
      endDate = Date.from(startDate.toInstant.plus(daysWindow - 1, DAYS))
      request = Request[F](
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
      openMeteoResponse <- client.expect[OpenMeteoResponse](request)(jsonOf[F, OpenMeteoResponse])

      _ <- logger.info("Converting weather api response into an email friendly output")
      dailyForecast <- extractDailyForecast(startDate, daysWindow, openMeteoResponse)
    } yield dailyForecast

    forecast.onError(error =>
      logger.error(s"Failed to retrieve weather forecast. Error = $error")
    )
  }
}