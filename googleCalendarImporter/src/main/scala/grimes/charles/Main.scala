package grimes.charles

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import grimes.charles.calendar.CalendarService
import grimes.charles.credentials.CredentialsLoader
import org.apache.logging.log4j.{LogManager, Logger}

import java.util
import scala.jdk.CollectionConverters.*

class Main extends RequestHandler[util.HashMap[String, String], String] {
  // Todo: Look into a cats effect friendly logger
  private given logger: Logger = LogManager.getLogger(this.getClass)

  def handleRequest(input: util.HashMap[String, String], context: Context): String =
    // Todo: See if there's a way to not need to call unsafeRunSync()
    run(input.asScala.toMap, context).unsafeRunSync()

  private def run(input: Map[String, String], context: Context): IO[String] = 
    // Todo: Look into best way to handle errors
    for {
      _ <- IO(logger.info(s"input = $input"))
      _ <- IO(logger.info(s"name = ${context.getFunctionName}"))
      _ <- IO(logger.info(s"aws request id = ${context.getAwsRequestId}"))
      _ <- IO(logger.info(s"version = ${context.getFunctionVersion}"))

      credentialsName <- IO(sys.env("CREDENTIALS_NAME"))
      awsSessionToken <- IO(sys.env("AWS_SESSION_TOKEN"))
      ownerEmail <- IO(sys.env("OWNER_EMAIL"))
      projectName <- IO(sys.env("GOOGLE_PROJECT_NAME"))
      
      _ <- IO(logger.info("Loading google credentials"))
      credentials <- CredentialsLoader.load(credentialsName, awsSessionToken)

      _ <- IO(logger.info("Retrieving google calendar events"))
      events <- CalendarService.retrieveEvents(credentials, projectName, ownerEmail)
    
      // Todo: Send events data in format expected by email builder lambda
    } yield events.toPrettyString
}