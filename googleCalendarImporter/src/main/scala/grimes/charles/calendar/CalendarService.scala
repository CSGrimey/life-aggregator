package grimes.charles.calendar

import cats.effect.{Clock, Sync}
import cats.syntax.all.*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Events
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.apache.logging.log4j.Logger

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*
import java.util.Date

object CalendarService {
  private def buildCalendarService[F[_]: Sync](credentials: GoogleCredentials, projectName: String): F[Calendar] =
    Sync[F].blocking(GoogleNetHttpTransport.newTrustedTransport()).map { httpTransport =>
      Calendar
        .Builder(
          httpTransport,
          GsonFactory.getDefaultInstance,
          HttpCredentialsAdapter(credentials)
        )
        .setApplicationName(projectName)
        .build()
    }

  private def retrieveEvents[F[_]: Sync](calendarService: Calendar, ownerEmail: String)
                                        (using clock: Clock[F], logger: Logger): F[Events] =
    for {
      now <- clock.realTimeInstant
      timeMin = Date.from(now)
      timeMax = Date.from(now.plus(7, DAYS)) // Todo: Make this configurable.

      _ <- Sync[F].delay(logger.info(s"Retrieving events from $timeMin to $timeMax"))
      eventsRequest = calendarService
        .events()
        .list(ownerEmail)
        .setSingleEvents(true)
        .setOrderBy("starttime")
        .setTimeMin(DateTime(timeMin))
        .setTimeMax(DateTime(timeMax))
        .setTimeZone("Europe/London")
        .setMaxResults(10)
      events <- Sync[F].blocking(eventsRequest.execute())
    } yield events

  def retrieveEvents[F[_]: Sync](credentials: GoogleCredentials, projectName: String, ownerEmail: String)
                                (using clock: Clock[F], logger: Logger): F[Events] = {
    val calendarEvents = for {
      _ <- Sync[F].delay(logger.info("Building google calendar service using access token"))
      calendarService <- buildCalendarService(credentials, projectName)

      events <- retrieveEvents(calendarService, ownerEmail)
    } yield events

    calendarEvents.onError(error =>
      Sync[F].delay(logger.error(s"Failed to retrieve calendar events. Error = $error"))
    )
  }
}
