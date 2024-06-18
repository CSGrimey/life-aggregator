package grimes.charles

import grimes.charles.common.models.AggregatedData
import grimes.charles.models.EmailContent

import java.util.Date

object EmailContentBuilder {
  // Todo: Handle multiple aggregated data sources
  def build(aggregatedData: List[AggregatedData], date: Date): EmailContent = {
    val subject = s"Life aggregator summary (${date.toString})"

    val body = aggregatedData match {
      case head :: _ =>
        s"<p>Aggregated data from ${head.aggregationType}</p><p>${head.aggregationResults.mkString("\n")}</p>"
      case Nil => "No results returned from all integrations"
    }

    EmailContent(subject, body)
  }
}