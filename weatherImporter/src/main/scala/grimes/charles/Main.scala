package grimes.charles

import cats.effect.IO
import cats.effect.kernel.Clock
import cats.effect.unsafe.implicits.*
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import grimes.charles.common.models.{AggregatedData, InvocationData}
import grimes.charles.weather.WeatherService
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.{InputStream, OutputStream}
import scala.io.Source
import java.nio.charset.StandardCharsets.UTF_8
import io.circe.parser.decode
import org.http4s.ember.client.EmberClientBuilder

class Main extends RequestStreamHandler {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit =
    run(inputStream, outputStream, context).unsafeRunSync()

  private def run(inputStream: InputStream, outputStream: OutputStream, context: Context): IO[Unit] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        for {
          _ <- logger.info("Reading invocation data")
          input <- IO(Source.fromInputStream(inputStream, UTF_8.name).mkString)
          invocationData <- IO.fromEither(decode[InvocationData](input))
          _ <- IO.raiseWhen(invocationData.daysWindow <= 0)
                           (RuntimeException("daysWindow must be positive"))

          now <- Clock[IO].realTimeInstant
          dailyForecast <- WeatherService[IO].getForecast(now, invocationData.daysWindow, client)

          result = AggregatedData(
            invocationData.daysWindow,
            aggregationType = "Weather forecast in Reading",
            aggregationResults = dailyForecast
              .toList
              .map(dayForecast => {
                import dayForecast._
                
                s"""
                ${date.toString}
                Sunrise = $sunrise | Sunset = $sunset
                ${hourlyForecast.toList.map(hf => {import hf._; s"$time $temperature $weather"}).mkString("\\n")}
                
                """
              })
          )
          resultJson <- IO(AggregatedData.encoder.apply(result))
          _ <- IO.blocking(outputStream.write(resultJson.toString.getBytes(UTF_8.name)))
          _ <- IO.blocking(outputStream.flush())
        } yield ()
      }
}