package grimes.charles

import grimes.charles.common.models.AggregatedData
import grimes.charles.common.utils.OutputsDate
import grimes.charles.models.EmailContent

import java.time.LocalDate

object EmailContentBuilder extends OutputsDate {
  private def buildSubject(aggregatedData: List[AggregatedData], date: LocalDate): String = {
    val daysWindow = aggregatedData
      .collectFirst(_.daysWindow)
      .getOrElse(0)
    val cadence =  daysWindow match {
      case 1 => "daily "
      case 7 => "weekly "
      case _ => ""
    }

    s"Life aggregator ${cadence}summary (${date.format(dateFormatter)})"
  }

  def build(aggregatedData: List[AggregatedData], date: LocalDate): EmailContent = {
    val subject = buildSubject(aggregatedData, date)

    aggregatedData match {
      case Nil => EmailContent(
        subject,
        body = "No results returned from all integrations"
      )
      case nonEmptyAggData =>
        val body = nonEmptyAggData
          .map(data => s"<p><b>${data.aggregationType}</b></p>${data.aggregationResults.mkString("<p>", "<br>", "</p>")}")
          .mkString

        EmailContent(subject, body)
    }
  }
}