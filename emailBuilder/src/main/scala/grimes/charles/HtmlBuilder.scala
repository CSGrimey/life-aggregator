package grimes.charles

import scalatags.Text.all.*
import grimes.charles.common.models.AggregatedData

object HtmlBuilder {
  // Todo: Update the html once I know what SES expects
  // Todo: Handle multiple aggregated data sources
  def build(aggregatedData: List[AggregatedData]): String =
    aggregatedData match {
      case head :: _ =>
        val content = head.aggregationResults.map(p(_))

        html(
          body(
            h1(s"Aggregated data from ${head.aggregationType}"),
            div(content)
          )
        ).render
      case Nil =>
        html(
          body(
            h1("No results returned from all integrations")
          )
        ).render
    }
}