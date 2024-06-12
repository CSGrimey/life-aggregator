package grimes.charles

import cats.effect.IO
import cats.effect.kernel.Clock
import cats.effect.unsafe.implicits.*
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import grimes.charles.calendar.CalendarService
import grimes.charles.common.models.AggregatedData
import grimes.charles.credentials.CredentialsLoader
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class Main extends RequestHandler[Object, String] {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  def handleRequest(input: Object, context: Context): String =
    run(input, context).unsafeRunSync()

  private def run(input: Object, context: Context): IO[String] =
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

          credentials <- CredentialsLoader[IO].load(
            credentialsName, awsSessionToken, client
          )

          now <- Clock[IO].realTimeInstant
          daysWindow = 1  // Todo: Read this from event
          events <- CalendarService[IO].retrieveEvents(
            credentials, projectName, ownerEmail, now, daysWindow
          )

          result = AggregatedData(
            daysWindow,
            aggregationType = "Google calendar events",
            aggregationResults = events.map(event =>
              s"${event.description} - ${event.startTime.toString}"
            )
          )
          resultJson <- IO(AggregatedData.encoder.apply(result))
        } yield resultJson.toString
      }
}