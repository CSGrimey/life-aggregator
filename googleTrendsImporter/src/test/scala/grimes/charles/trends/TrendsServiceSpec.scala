package grimes.charles.trends

import cats.effect.IO
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.QueryJobConfiguration
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite

object TrendsServiceSpec extends SimpleIOSuite {
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val retrievedTerms = List("UFC", "Mickey mouse", "Frozen")

  private val expectedQuery =
    """
      SELECT
        refresh_date AS Day,
        term AS Top_Term,
        rank,
        country_name,
        region_name,
      FROM `bigquery-public-data.google_trends.international_top_terms`
      WHERE
        rank BETWEEN 1 AND 10
        AND country_name = 'United Kingdom'
        AND region_name = 'England'
        AND refresh_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
      GROUP BY rank, Day, Top_Term, country_name, region_name
      ORDER BY rank
      LIMIT 10
    """

  private def buildTrendsServiceStub(
                                      daysWindow: Int,
                                      retrievedTrends: List[String]
                                    ): TrendsService[IO] =
    new TrendsService[IO] {
      override protected def executeQuery(
                                           credentials: GoogleCredentials,
                                           queryConfig: QueryJobConfiguration
                                         ): IO[List[String]] =
        IO.raiseUnless(
          queryConfig.getQuery == expectedQuery
        )(RuntimeException("Incorrect query params"))
        .as(retrievedTrends)
    }

  test("Should retrieve trends") {
    val trendsServiceStub = buildTrendsServiceStub(
      daysWindow = 1, retrievedTerms
    )

    for {
      trends <- trendsServiceStub.retrieveTrends(
        new GoogleCredentials {}, daysWindow = 1
      )
    } yield expect(trends == retrievedTerms)
  }
}