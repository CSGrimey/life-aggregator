package grimes.charles

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import grimes.charles.common.models.AggregatedData
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe.parser.*

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import scala.io.Source

class Main extends RequestStreamHandler {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit =
    run(inputStream, outputStream, context).unsafeRunSync()

  private def run(inputStream: InputStream, outputStream: OutputStream, context: Context): IO[Unit] =
    for {
      _ <- logger.info("Reading aggregated data")
      input <- IO(Source.fromInputStream(inputStream, UTF_8.name).mkString)
      aggregatedData <- IO.fromEither(decode[StepFunctionInput](input)).map(_.input)

      _ <- logger.info("Building HTML using aggregated data")
      emailHtml = HtmlBuilder.build(List(aggregatedData))
      
      _ <- IO.blocking(outputStream.write(emailHtml.getBytes(UTF_8.name)))
      _ <- IO.blocking(outputStream.flush())
    } yield ()
}