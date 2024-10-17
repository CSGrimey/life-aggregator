package grimes.charles.weather

import cats.effect.IO
import cats.effect.kernel.Resource
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.IOSuite

import java.nio.charset.StandardCharsets.*
import java.time.Instant
import scala.io.Source

object WeatherServiceSpec extends IOSuite {
  override type Res = String

  override def sharedResource: Resource[IO, String] = Resource.fromAutoCloseable(
    // TODO: Make code smart enough to grab daysWindow worth of data from a week forecast
    IO.blocking(Source.fromURL(getClass.getResource("/next_week_weather.json"), UTF_8.name))
  ).map(_.mkString)

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val now = Instant.parse("2024-06-24T10:00:00.000+01:00")

  private val expectedHeaders = Headers(
    Header.Raw(ci"Accept", "application/json")
  )

  Seq((1, "2024-06-25"), (7, "2024-07-01")).foreach { (daysWindow, expectedEndDate) =>
    test("Should retrieve the weather forecast") { forecastResponse =>
      val clientStub = Client.apply[IO] { request =>
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
            case _ => Ok(forecastResponse)
          }
        }
      }

      for {
        result <- WeatherService[IO].getForecast(now, daysWindow, clientStub)
      } yield expect(result.nonEmpty)
    }
  }
}