package grimes.charles

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.apache.logging.log4j.LogManager

import java.util

class Main extends RequestHandler[util.HashMap[String, String], Unit] {
  private val logger = LogManager.getLogger(this.getClass)

  def handleRequest(input: util.HashMap[String, String], context: Context): Unit = {
    logger.info(s"input = $input")
    logger.info(s"name = ${context.getFunctionName}")
    logger.info(s"aws request id = ${context.getAwsRequestId}")
    logger.info(s"version = ${context.getFunctionVersion}")
  }
}