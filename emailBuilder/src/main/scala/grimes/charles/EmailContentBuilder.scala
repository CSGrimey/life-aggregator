package grimes.charles

import grimes.charles.common.models.AggregatedData
import grimes.charles.models.EmailContent

import java.util.Date

object EmailContentBuilder {
  def build(aggregatedData: List[AggregatedData], date: Date): EmailContent = {
    val subject = s"Life aggregator summary (${date.toString})"

    aggregatedData match {
      case Nil => EmailContent(
        subject,
        body = "No results returned from all integrations"
      )
      case nonEmptyAggData =>
        val body = nonEmptyAggData
          .map(data => s"<p><b>Aggregated data from ${data.aggregationType}</b></p>${data.aggregationResults.mkString("<p>", "<br>", "</p>")}")
          .mkString

        EmailContent(
          subject,
          body
        )
    }
  }
}