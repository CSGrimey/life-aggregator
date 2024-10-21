package grimes.charles.common.utils

import java.time.{Instant, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.util.Date

trait DateUtils {
  private val dateTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm") 
  protected val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  protected val timeZone = "Europe/London"

  protected def toStartOfNextDay(now: Instant): Date = Date.from(now.plus(1, DAYS).truncatedTo(DAYS))
  
  protected def getTimeFromDateTimeString(dateTimeString: String): Option[String] =
    Option(LocalDateTime.parse(dateTimeString, dateTimeformatter).toLocalTime.toString)
}