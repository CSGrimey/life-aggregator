package grimes.charles.calendar

import cats.effect.{Clock, IO}
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
  private def buildCalendarService(credentials: GoogleCredentials, projectName: String): IO[Calendar] =
    IO.blocking(GoogleNetHttpTransport.newTrustedTransport()).map { httpTransport =>
      Calendar
        .Builder(
          httpTransport,
          GsonFactory.getDefaultInstance,
          HttpCredentialsAdapter(credentials)
        )
        .setApplicationName(projectName)
        .build()
    }

  def retrieveEvents(credentials: GoogleCredentials, projectName: String, ownerEmail: String)
                    (using clock: Clock[IO], logger: Logger): IO[Events] =
    for {
      calendarService <- buildCalendarService(credentials, projectName)

      now <- clock.realTimeInstant
      timeMin = Date.from(now)
      timeMax = Date.from(now.plus(7, DAYS))  // Todo: Make this configurable.
      
      _ <- IO(logger.info(s"Retrieving events from $timeMin to $timeMax"))
      
      eventsRequest = calendarService
        .events()
        .list(ownerEmail)
        .setSingleEvents(true)
        .setOrderBy("starttime")
        .setTimeMin(DateTime(timeMin))
        .setTimeMax(DateTime(timeMax)) 
        .setTimeZone("Europe/London")
        .setMaxResults(10)
      events <- IO.blocking(eventsRequest.execute())
    } yield events
}
