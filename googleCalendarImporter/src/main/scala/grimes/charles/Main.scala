package grimes.charles

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import grimes.charles.calendar.CalendarService
import grimes.charles.credentials.CredentialsLoader
import org.apache.logging.log4j.{LogManager, Logger}
import org.http4s.ember.client.EmberClientBuilder

import java.util
import scala.jdk.CollectionConverters.*

class Main extends RequestHandler[util.HashMap[String, String], String] {
  // Todo: Look into a cats effect friendly logger
  private given logger: Logger = LogManager.getLogger(this.getClass)

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
          _ <- IO(logger.info("Reading env vars"))
          credentialsName <- IO(sys.env("CREDENTIALS_NAME"))
          awsSessionToken <- IO(sys.env("AWS_SESSION_TOKEN"))
          ownerEmail <- IO(sys.env("OWNER_EMAIL"))
          projectName <- IO(sys.env("GOOGLE_PROJECT_NAME"))

          credentials <- CredentialsLoader.load[IO](
            credentialsName, awsSessionToken, client
          )

          events <- CalendarService.retrieveEvents[IO](
            credentials, projectName, ownerEmail
          )

          // Todo: Send events data in format expected by email builder lambda
        } yield events.toPrettyString
      }
}