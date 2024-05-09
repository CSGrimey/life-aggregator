package grimes.charles

import com.amazonaws.services.lambda.runtime.Context

object Main extends App {
  def myHandler(input: String, context: Context): String = s"Received input = $input"
}