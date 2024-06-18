package grimes.charles

import grimes.charles.common.models.AggregatedData
import grimes.charles.models.EmailContent

import java.util.Date

object EmailContentBuilder {
  // Todo: Handle multiple aggregated data sources
  def build(aggregatedData: List[AggregatedData], date: Date): EmailContent = {
    val subject = s"Life aggregator summary (${date.toString})"

    aggregatedData match {
      case head :: _ =>
        val body =
          s"Aggregated data from ${head.aggregationType}\n${head.aggregationResults.mkString("\n")}"

        EmailContent(
          subject = subject,
          body = body
        )
      case Nil =>
        EmailContent(
          subject = subject,
          body = "No results returned from all integrations",
        )
    }
  }
}