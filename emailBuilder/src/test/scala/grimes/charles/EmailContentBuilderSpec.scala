package grimes.charles

import grimes.charles.EmailContentBuilder.build
import grimes.charles.common.models.AggregatedData
import grimes.charles.models.EmailContent
import weaver.{Expectations, SimpleIOSuite}

import java.time.LocalDate

object EmailContentBuilderSpec extends SimpleIOSuite {
  private val date = LocalDate.parse("2024-06-24")

  pureTest("Should build valid daily summary HTML from aggregated data") {
    runTest(daysWindow = 1, expectedEmailSubject = "Life aggregator daily summary (24/06/2024)")
  }

  pureTest("Should build valid weekly summary HTML from aggregated data") {
    runTest(daysWindow = 7, expectedEmailSubject = "Life aggregator weekly summary (24/06/2024)")
  }

  pureTest("Should build valid summary HTML from aggregated data") {
    runTest(daysWindow = 3, expectedEmailSubject = "Life aggregator summary (24/06/2024)")
  }

  private def runTest(daysWindow: Int, expectedEmailSubject: String): Expectations = {
    val aggregatedData = List(
      AggregatedData(
        daysWindow,
        aggregationType = "Unit test 1",
        aggregationResults = List(
          "Some data 1",
          "Some data 3",
          "Some data 2"
        )
      ),
      AggregatedData(
        daysWindow,
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

    expect(
      emailContentResult ==
        EmailContent(expectedEmailSubject, expectedEmailBody)
    )
  }

  pureTest("Should build valid HTML from empty aggregated data") {
    expect(
      build(List(), date)
        == EmailContent(
          "Life aggregator summary (24/06/2024)",
          "No results returned from all integrations"
      )
    )
  }
}