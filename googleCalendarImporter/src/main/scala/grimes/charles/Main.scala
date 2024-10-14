package grimes.charles

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Clock
import cats.effect.unsafe.implicits.*
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import grimes.charles.calendar.CalendarService
import grimes.charles.common.google.credentials.CredentialsLoader
import grimes.charles.common.models.{AggregatedData, InvocationData}
import grimes.charles.common.ssm.ParamsStore
import io.circe.parser.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.google.api.services.calendar.CalendarScopes.CALENDAR_EVENTS_READONLY

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source

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
          _ <- logger.info("Reading env vars")
          credentialsName <- IO(sys.env("CREDENTIALS_NAME"))
          awsSessionToken <- IO(sys.env("AWS_SESSION_TOKEN"))
          ownerEmail <- IO(sys.env("OWNER_EMAIL"))
          projectName <- IO(sys.env("GOOGLE_PROJECT_NAME"))

          _ <- logger.info("Reading invocation data")
          input <- IO(Source.fromInputStream(inputStream, UTF_8.name).mkString)
          invocationData <- IO.fromEither(decode[InvocationData](input))
          _ <- IO.raiseWhen(invocationData.daysWindow <= 0)
                           (RuntimeException("daysWindow must be positive"))
          
          _ <- logger.info("Retrieving credentials from params store")
          serviceAccountCredsParam <- ParamsStore[IO].get(
            credentialsName, awsSessionToken, client
          )
          
          accessToken <- CredentialsLoader[IO].load(
            serviceAccountCredsParam,
            NonEmptyList.of(
              "https://www.googleapis.com/auth/cloud-platform.read-only",
              CALENDAR_EVENTS_READONLY
            )
          )

          now <- Clock[IO].realTimeInstant
          events <- CalendarService[IO].retrieveEvents(
            accessToken, projectName, ownerEmail, now, invocationData.daysWindow
          )

          result = AggregatedData(
            invocationData.daysWindow,
            aggregationType = "Google calendar events",
            aggregationResults = events.map(event =>
              s"${event.description} - ${event.startDate} ${event.timeRange.getOrElse("")}"
            )
          )
          resultJson <- IO(AggregatedData.encoder.apply(result))
          _ <- IO.blocking(outputStream.write(
            resultJson.toString.getBytes(UTF_8.name))
          )
          _ <- IO.blocking(outputStream.flush())
        } yield ()
      }
}