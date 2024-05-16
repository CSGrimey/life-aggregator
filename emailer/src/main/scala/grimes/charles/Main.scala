package grimes.charles

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.apache.logging.log4j.LogManager

class Main extends RequestHandler[String, Unit] {
  private val logger = LogManager.getLogger(this.getClass)

  def handleRequest(input: String, context: Context): Unit =
    logger.info(s"input = $input")
}