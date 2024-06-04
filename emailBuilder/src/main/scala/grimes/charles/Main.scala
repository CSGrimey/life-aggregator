package grimes.charles

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util
import scala.jdk.CollectionConverters.*

class Main extends RequestHandler[util.HashMap[String, String], Unit] {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  def handleRequest(input: util.HashMap[String, String], context: Context): Unit =
    // Todo: See if there's a way to not need to call unsafeRunSync()
    run(input.asScala.toMap, context).unsafeRunSync()

  private def run(input: Map[String, String], context: Context): IO[Unit] =
    for {
      _ <- logger.info(s"input = $input")
      _ <- logger.info(s"name = ${context.getFunctionName}")
      _ <- logger.info(s"aws request id = ${context.getAwsRequestId}")
      _ <- logger.info(s"version = ${context.getFunctionVersion}")
    } yield ()
}