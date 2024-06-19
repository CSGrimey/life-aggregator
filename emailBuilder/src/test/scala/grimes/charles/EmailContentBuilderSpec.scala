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
    val aggregatedData = List(
      AggregatedData(
        daysWindow = 3,
        aggregationType = "Unit test 1",
        aggregationResults = List(
          "Some data 1",
          "Some data 3",
          "Some data 2"
        )
      ),
      AggregatedData(
        daysWindow = 3,
        aggregationType = "Unit test 2",
        aggregationResults = List(
          "Some data 1",
          "Some data 2"
        )
      )
    )

    val emailContentResult = build(aggregatedData, date)

    val expectedEmailBody =
      "<p><b>Aggregated data from Unit test 1</b></p><p>Some data 1<br>Some data 3<br>Some data 2</p><p><b>Aggregated data from Unit test 2</b></p><p>Some data 1<br>Some data 2</p>"

    expect(emailContentResult == EmailContent(expectedEmailSubject, expectedEmailBody))
  }

  pureTest("Should build valid HTML from empty aggregated data") {
    expect(build(List(), date) == EmailContent(expectedEmailSubject, "No results returned from all integrations"))
  }
}