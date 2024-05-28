package grimes.charles.calendar

import cats.effect.IO
import com.google.api.services.calendar.{Calendar, CalendarScopes}
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import grimes.charles.credentials.CredentialsLoader
import com.google.api.services.calendar.model.Events
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar
import java.util.Date
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*

object CalendarService {
  private def buildCalendarService(credentials: GoogleCredentials, projectName: String): Calendar = 
    Calendar
      .Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance,
        HttpCredentialsAdapter(credentials)
      )
      .setApplicationName(projectName)
      .build()

  def retrieveEvents(credentials: GoogleCredentials, projectName: String, ownerEmail: String): IO[String] = 
    // Todo: Handle this better
    IO {
      buildCalendarService(credentials, projectName)
        .events()
        .list(ownerEmail)
        .setSingleEvents(true)
        .setOrderBy("starttime")
        .setTimeMin(DateTime(Date.from(Instant.now())))
        .setTimeMax(DateTime(Date.from(Instant.now().plus(7, DAYS)))) // Todo: Make this configurable.
        .setTimeZone("Europe/London")
        .setMaxResults(10)
        .execute()
        .toPrettyString
    }
}
