package grimes.charles

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.apache.logging.log4j.LogManager

class Main extends RequestHandler[String, Unit] {
  private val logger = LogManager.getLogger(this.getClass)

  def handleRequest(input: String, context: Context): Unit = {
    logger.info(s"input received = $input")
    logger.info(s"aws request id = ${context.getAwsRequestId}")
    logger.info(s"version = ${context.getFunctionVersion}")
    logger.info(s"name = ${context.getFunctionName}")
  }
}