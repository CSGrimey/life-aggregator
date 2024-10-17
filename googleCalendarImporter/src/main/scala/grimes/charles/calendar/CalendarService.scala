package grimes.charles.calendar

import cats.effect.Sync
import cats.syntax.all.*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Events
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import grimes.charles.common.utils.DateUtils
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Date
import scala.jdk.CollectionConverters.*

class CalendarService[F[_]: Sync] extends DateUtils {
  private def buildCalendarService(credentials: GoogleCredentials, projectName: String): F[Calendar] =
    Sync[F]
      .delay(GoogleNetHttpTransport.newTrustedTransport())
      .map { httpTransport =>
        Calendar
          .Builder(
            httpTransport,
            GsonFactory.getDefaultInstance,
            HttpCredentialsAdapter(credentials)
          )
          .setApplicationName(projectName)
          .build()
      }

  private def retrieveEvents(
                              calendarService: Calendar,
                              ownerEmail: String,
                              now: Instant,
                              daysWindow: Int
                            )(using logger: Logger[F]): F[Events] =
    for {
      timeMin <- Sync[F].delay(toStartOfNextDay(now))
      timeMax = Date.from(timeMin.toInstant.plus(daysWindow, DAYS))

      _ <- logger.info(s"Retrieving events from $timeMin to $timeMax")
      eventsRequest = calendarService
        .events()
        .list(ownerEmail)
        .setSingleEvents(true)
        .setOrderBy("starttime")
        .setTimeMin(DateTime(timeMin))
        .setTimeMax(DateTime(timeMax))
        .setTimeZone(timeZone)
      events <- executeRequest(eventsRequest)
    } yield events

  private def summariseEvents(events: Events): List[EventSummary] =
    events.getItems match {
      case null => List()
      case items =>
        items
          .asScala
          .map(event =>
            // All day events return a different field value (date) than regular events (dateTime)
            Option(event.getStart.getDate) match {
              case Some(startDate) =>
                EventSummary(event.getSummary, toDateString(startDate), timeRange = None)
              case None =>
                val startDate = toDateString(event.getStart.getDateTime)
                val startTime = toTimeString(event.getStart.getDateTime)
                val endTime = toTimeString(event.getEnd.getDateTime)
                val timeRange = s"$startTime-$endTime"

                EventSummary(event.getSummary, startDate, timeRange.some)
            }
          ).toList
    }

  private def toDateString(date: DateTime) = toFormattedString(date.getValue, dateFormatter)
  private def toTimeString(date: DateTime) = toFormattedString(date.getValue, DateTimeFormatter.ofPattern("H:mm"))
  private def toFormattedString(timestamp: Long, formatter: DateTimeFormatter): String =
    ZonedDateTime
      .ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of(timeZone))
      .format(formatter)
    
  protected def executeRequest(request: Calendar#Events#List): F[Events] =
    Sync[F].blocking(request.execute())

  def retrieveEvents(
                      credentials: GoogleCredentials,
                      projectName: String,
                      ownerEmail: String,
                      now: Instant,
                      daysWindow: Int
                    )(using logger: Logger[F]): F[List[EventSummary]] = {
    val calendarEvents = for {
      _ <- logger.info("Building google calendar service using access token")
      calendarService <- buildCalendarService(credentials, projectName)

      events <- retrieveEvents(calendarService, ownerEmail, now, daysWindow)
      summarisedEvents = summariseEvents(events)
      _ <- logger.info(s"Retrieved ${summarisedEvents.size} events")
    } yield summarisedEvents

    calendarEvents.onError(error =>
      logger.error(s"Failed to retrieve calendar events. Error = $error")
    )
  }
}