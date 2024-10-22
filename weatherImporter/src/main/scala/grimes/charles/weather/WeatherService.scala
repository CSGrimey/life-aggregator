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
import java.time.*
import java.util.Date

class WeatherService[F[_] : Async] extends DateUtils {
  // https://open-meteo.com/en/docs
  private val openMeteoUrl = uri"https://api.open-meteo.com/v1/forecast"

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  private val startHour = 8 // Start daily forecast at 08:00
  private val hoursInDay = 24

  private def extractDailyForecast(startDate: Date, daysWindow: Int, openMeteoResponse: OpenMeteoResponse): F[NonEmptyList[DayForecast]] = {
    import openMeteoResponse._

    def extractHourlyForecast(day: Int): F[NonEmptyList[HourForecast]] =
      (startHour until hoursInDay)
        .toList
        .traverse(hour => {
          val index = (day * hoursInDay) + hour

          for {
            temperature <- Async[F].fromOption(
              hourly.temperature_2m.get(index).map(t => s"${Math.round(t)}°C"),
              RuntimeException(s"Failed to extract temperature for index $index")
            )
            weather <- Async[F].fromOption(
              hourly.weather_code.get(index).flatMap(WmoCodes.mapping.get),
              RuntimeException(s"Failed to extract weather code for index $index")
            )
          } yield HourForecast(temperature, time = LocalTime.of(hour, 0), weather)
        }).flatMap(hf =>
          Async[F].fromOption(NonEmptyList.fromList(hf), RuntimeException(s"Failed to extract hourly forecast for day $day"))
        )

    val dailyForecast = (0 until daysWindow)
      .toList
      .traverse(day =>
        for {
          sunrise <- Async[F].fromOption(daily.sunrise.get(day).flatMap(getTimeFromDateTimeString), RuntimeException(s"Failed to extract sunrise for day $day"))
          sunset <- Async[F].fromOption(daily.sunset.get(day).flatMap(getTimeFromDateTimeString), RuntimeException(s"Failed to extract sunset for day $day"))

          hourlyForecast <- extractHourlyForecast(day)
        } yield DayForecast(Date.from(startDate.toInstant.plus(day, DAYS)), sunrise, sunset, hourlyForecast)
      )

    dailyForecast.flatMap(df => Async[F].fromOption(NonEmptyList.fromList(df), RuntimeException("Failed to extract daily forecast")))
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