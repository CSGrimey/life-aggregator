package grimes.charles

import cats.effect.IO
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util

class Main extends RequestHandler[util.HashMap[String, String], Unit] {
  private given logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger

  def handleRequest(input: util.HashMap[String, String], context: Context): Unit = {
    for {
      _ <- logger.info(s"input = $input")
      _ <- logger.info(s"name = ${context.getFunctionName}")
      _ <- logger.info(s"aws request id = ${context.getAwsRequestId}")
      _ <- logger.info(s"version = ${context.getFunctionVersion}")
    } yield ()
  }
}