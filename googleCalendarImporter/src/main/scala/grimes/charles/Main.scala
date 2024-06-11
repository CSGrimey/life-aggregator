package grimes.charles

import cats.effect.IO
import cats.effect.kernel.Clock
import cats.effect.unsafe.implicits.*
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import grimes.charles.calendar.CalendarService
import grimes.charles.credentials.CredentialsLoader
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util
import scala.jdk.CollectionConverters.*

class Main extends RequestHandler[util.HashMap[String, String], String] {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  def handleRequest(input: util.HashMap[String, String], context: Context): String = 
    // Todo: See if there's a way to not need to call unsafeRunSync()
    run(input.asScala.toMap, context).unsafeRunSync()

  private def run(input: Map[String, String], context: Context): IO[String] = 
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        // Todo: Look into best way to handle errors
        for {
          // Todo: Use config loading library
          _ <- logger.info("Reading env vars")
          credentialsName <- IO(sys.env("CREDENTIALS_NAME"))
          awsSessionToken <- IO(sys.env("AWS_SESSION_TOKEN"))
          ownerEmail <- IO(sys.env("OWNER_EMAIL"))
          projectName <- IO(sys.env("GOOGLE_PROJECT_NAME"))

          credentials <- CredentialsLoader[IO].load(
            credentialsName, awsSessionToken, client
          )

          now <- Clock[IO].realTimeInstant
          events <- CalendarService[IO].retrieveEvents(
            credentials, projectName, ownerEmail, now, 1
          )

          // Todo: Send events data in format expected by email builder lambda
          _ <- logger.info(s"events = $events")
        } yield events.mkString
      }
}