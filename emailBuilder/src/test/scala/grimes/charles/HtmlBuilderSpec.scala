package grimes.charles

import grimes.charles.common.models.AggregatedData
import weaver.SimpleIOSuite

object HtmlBuilderSpec extends SimpleIOSuite {
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
    val htmlResult = HtmlBuilder.build(List(aggregatedData))

    val expectedHtml = "<html><body><h1>Aggregated data from Unit test</h1><div><p>Some data 1</p><p>Some data 3</p><p>Some data 2</p></div></body></html>"

    expect(htmlResult == expectedHtml)
  }

  pureTest("Should build valid HTML from empty aggregated data") {
    expect(HtmlBuilder.build(List()) == "<html><body><h1>No results returned from all integrations</h1></body></html>")
  }
}