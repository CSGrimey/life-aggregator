package grimes.charles.common.utils

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.util.Date

trait DateUtils {
  protected val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  protected val timeZone = "Europe/London"

  protected def toStartOfNextDay(now: Instant): Date =
    Date.from(now.plus(1, DAYS).truncatedTo(DAYS))
}