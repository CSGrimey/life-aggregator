package grimes.charles

import grimes.charles.EmailContentBuilder.build
import grimes.charles.common.models.AggregatedData
import grimes.charles.models.EmailContent
import weaver.SimpleIOSuite

import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

object EmailContentBuilderSpec extends SimpleIOSuite {
  private val date = Date.from(LocalDateTime.of(2024, 6, 24, 9, 0).toInstant(ZoneOffset.UTC))
  private val expectedEmailSubject = "Life aggregator summary (Mon Jun 24 10:00:00 IST 2024)"

  pureTest("Should build valid HTML from aggregated data") {
    val aggregatedData = AggregatedData(
      daysWindow = 3,
      aggregationType = "Unit test",
      aggregationResults = List(
        "Some data 1",
        "Some data 3",
        "Some data 2"
      )
    )

    val emailContentResult = build(List(aggregatedData), date)

    val expectedEmailBody =
      "Aggregated data from Unit test\nSome data 1\nSome data 3\nSome data 2"

    expect(emailContentResult == EmailContent(expectedEmailSubject, expectedEmailBody))
  }

  pureTest("Should build valid HTML from empty aggregated data") {
    expect(build(List(), date) == EmailContent(expectedEmailSubject, "No results returned from all integrations"))
  }
}