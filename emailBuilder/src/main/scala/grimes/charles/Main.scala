package grimes.charles

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source

class Main extends RequestStreamHandler {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  def handleRequest(input: InputStream, outputStream: OutputStream, context: Context): Unit =
    run(input, outputStream, context).unsafeRunSync()

  private def run(input: InputStream, outputStream: OutputStream, context: Context): IO[OutputStream] =
    for {
      inputString <- IO(Source.fromInputStream(input, UTF_8.name).mkString)
      _ <- logger.info(s"input = $inputString")
      _ <- logger.info(s"name = ${context.getFunctionName}")
      _ <- logger.info(s"aws request id = ${context.getAwsRequestId}")
      _ <- logger.info(s"version = ${context.getFunctionVersion}")

      _ <- IO.blocking(outputStream.write(s"TODO: Send this input in html ($inputString)".getBytes(UTF_8.name)))
      _ <- IO.blocking(outputStream.flush())
    } yield outputStream
}