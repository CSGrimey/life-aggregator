package grimes.charles.weather

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Resource
import grimes.charles.weather.models.{DayForecast, HourForecast}
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.IOSuite

import java.nio.charset.StandardCharsets.*
import java.time.{Instant, LocalTime}
import java.util.Date
import scala.io.Source

object WeatherServiceSpec extends IOSuite {
  override type Res = String

  override def sharedResource: Resource[IO, String] = Resource.fromAutoCloseable(
    IO.blocking(Source.fromURL(getClass.getResource("/next_week_weather.json"), UTF_8.name))
  ).map(_.mkString)

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val now = Instant.parse("2024-06-24T10:00:00.000+01:00")

  private val expectedHeaders = Headers(
    Header.Raw(ci"Accept", "application/json")
  )

  private def buildClientStub(expectedEndDate: String, openMeteoResponse: String) = Client.apply[IO] { request =>
    Resource.eval {
      (
        request.headers == expectedHeaders,
        request.uri.toString == s"https://api.open-meteo.com/v1/forecast?latitude=51.454265&longitude=-0.97813&hourly=temperature_2m%2Cweather_code&daily=sunrise%2Csunset&timezone=Europe/London&start_date=2024-06-25&end_date=$expectedEndDate&models=ukmo_seamless"
      ) match {
        case (false, _) =>
          logger.error(s"Unexpected request headers (${request.headers})") >>
            BadRequest()
        case (true, false) =>
          logger.error(s"Unexpected URL (${request.uri.toString})") >>
            BadRequest()
        case _ => Ok(openMeteoResponse)
      }
    }
  }

  test("Should retrieve next single day weather forecast") { openMeteoResponse =>
    val clientStub = buildClientStub(expectedEndDate = "2024-06-25", openMeteoResponse)

    for {
      result <- WeatherService[IO].getForecast(now, daysWindow = 1, clientStub)

      expectedForecast = NonEmptyList.one(
        DayForecast(
          Date.from(Instant.parse("2024-06-25T01:00:00.000+01:00")),
          sunrise = "07:29",
          sunset = "18:02",
          hourlyForecast = NonEmptyList.of(
            HourForecast("15°C", LocalTime.of(8, 0), "Overcast"),
            HourForecast("15°C", LocalTime.of(9, 0), "Overcast"),
            HourForecast("15°C", LocalTime.of(10, 0), "Overcast"),
            HourForecast("16°C", LocalTime.of(11, 0), "Overcast"),
            HourForecast("16°C", LocalTime.of(12, 0), "Partly cloudy"),
            HourForecast("17°C", LocalTime.of(13, 0), "Overcast"),
            HourForecast("18°C", LocalTime.of(14, 0), "Overcast"),
            HourForecast("18°C", LocalTime.of(15, 0), "Overcast"),
            HourForecast("18°C", LocalTime.of(16, 0), "Partly cloudy"),
            HourForecast("18°C", LocalTime.of(17, 0), "Partly cloudy"),
            HourForecast("17°C", LocalTime.of(18, 0), "Partly cloudy"),
            HourForecast("17°C", LocalTime.of(19, 0), "Overcast"),
            HourForecast("16°C", LocalTime.of(20, 0), "Clear"),
            HourForecast("15°C", LocalTime.of(21, 0), "Mostly clear"),
            HourForecast("15°C", LocalTime.of(22, 0), "Partly cloudy"),
            HourForecast("14°C", LocalTime.of(23, 0), "Clear")
          )
        )
      )
    } yield expect(result == expectedForecast)
  }

  test("Should retrieve weather forecast for the next two days") { openMeteoResponse =>
    val clientStub = buildClientStub(expectedEndDate = "2024-06-26", openMeteoResponse)

    for {
      result <- WeatherService[IO].getForecast(now, daysWindow = 2, clientStub)

      expectedForecast = NonEmptyList.of(
        DayForecast(
          Date.from(Instant.parse("2024-06-25T01:00:00.000+01:00")),
          sunrise = "07:29",
          sunset = "18:02",
          hourlyForecast = NonEmptyList.of(
            HourForecast("15°C", LocalTime.of(8, 0), "Overcast"),
            HourForecast("15°C", LocalTime.of(9, 0), "Overcast"),
            HourForecast("15°C", LocalTime.of(10, 0), "Overcast"),
            HourForecast("16°C", LocalTime.of(11, 0), "Overcast"),
            HourForecast("16°C", LocalTime.of(12, 0), "Partly cloudy"),
            HourForecast("17°C", LocalTime.of(13, 0), "Overcast"),
            HourForecast("18°C", LocalTime.of(14, 0), "Overcast"),
            HourForecast("18°C", LocalTime.of(15, 0), "Overcast"),
            HourForecast("18°C", LocalTime.of(16, 0), "Partly cloudy"),
            HourForecast("18°C", LocalTime.of(17, 0), "Partly cloudy"),
            HourForecast("17°C", LocalTime.of(18, 0), "Partly cloudy"),
            HourForecast("17°C", LocalTime.of(19, 0), "Overcast"),
            HourForecast("16°C", LocalTime.of(20, 0), "Clear"),
            HourForecast("15°C", LocalTime.of(21, 0), "Mostly clear"),
            HourForecast("15°C", LocalTime.of(22, 0), "Partly cloudy"),
            HourForecast("14°C", LocalTime.of(23, 0), "Clear")
          )
        ),
        DayForecast(
          Date.from(Instant.parse("2024-06-26T01:00:00.000+01:00")),
          sunrise = "07:30",
          sunset = "18:00",
          hourlyForecast = NonEmptyList.of(
            HourForecast("11°C", LocalTime.of(8, 0), "Overcast"),
            HourForecast("11°C", LocalTime.of(9, 0), "Overcast"),
            HourForecast("11°C", LocalTime.of(10, 0), "Overcast"),
            HourForecast("12°C", LocalTime.of(11, 0), "Partly cloudy"),
            HourForecast("14°C", LocalTime.of(12, 0), "Clear"),
            HourForecast("15°C", LocalTime.of(13, 0), "Clear"),
            HourForecast("16°C", LocalTime.of(14, 0), "Clear"),
            HourForecast("17°C", LocalTime.of(15, 0), "Clear"),
            HourForecast("17°C", LocalTime.of(16, 0), "Partly cloudy"),
            HourForecast("16°C", LocalTime.of(17, 0), "Mostly clear"),
            HourForecast("16°C", LocalTime.of(18, 0), "Clear"),
            HourForecast("15°C", LocalTime.of(19, 0), "Clear"),
            HourForecast("14°C", LocalTime.of(20, 0), "Mostly clear"),
            HourForecast("14°C", LocalTime.of(21, 0), "Overcast"),
            HourForecast("14°C", LocalTime.of(22, 0), "Overcast"),
            HourForecast("13°C", LocalTime.of(23, 0), "Overcast")
          )
        )
      )
    } yield expect(result == expectedForecast)
  }
}