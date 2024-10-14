package grimes.charles.trends

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.bigquery.{BigQuery, BigQueryOptions, QueryJobConfiguration}
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import scala.jdk.CollectionConverters.*

class TrendsService[F[_]: Sync] {
  private def buildQuery(daysWindow: Int): String =
    s"""
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
        AND refresh_date >= DATE_SUB(CURRENT_DATE(), INTERVAL $daysWindow DAY)
      GROUP BY rank, Day, Top_Term, country_name, region_name
      ORDER BY rank
      LIMIT 10
    """
    
  protected def executeQuery(
                              credentials: GoogleCredentials,
                              queryConfig: QueryJobConfiguration
                            ): F[List[String]] =
    for {
      client <- Sync[F].delay(buildClient(credentials))
      
      tableResult <- Sync[F].blocking(client.query(queryConfig))
      results = tableResult
        .getValues
        .asScala
        .map(_.get("Top_Term").getStringValue)
        .toList
    } yield results
    
  private def buildClient(credentials: GoogleCredentials): BigQuery =
    BigQueryOptions
      .newBuilder()
      .setCredentials(credentials)
      .build()
      .getService
  
  def retrieveTrendsAsLinks(credentials: GoogleCredentials, daysWindow: Int)
                           (using logger: Logger[F]): F[NonEmptyList[String]] = {
    val trendsLinks = for {
      _ <- logger.info(s"Retrieving trends from the last $daysWindow days")

      topTermsInEnglandQuery = buildQuery(daysWindow)
      queryConfig = QueryJobConfiguration.of(topTermsInEnglandQuery)

      terms <- executeQuery(credentials, queryConfig)
      _ <- logger.info(s"Retrieved ${terms.size} trends")

      termsAsLinks = NonEmptyList.fromList(
        terms.map(trend =>
          s"<a href=\"https://search.brave.com/search?q=${trend.replace(" ", "+")}\">$trend</a>"
        )
      ).getOrElse(NonEmptyList.one("No trends returned by Google"))
    } yield termsAsLinks

    trendsLinks.onError(error =>
      logger.error(s"Failed to retrieve trends. Error = $error")
    )
  }
}