package grimes.charles.calendar

import cats.effect.IO
import com.google.api.client.util.DateTime as GoogleDateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.{Event, EventDateTime, Events}
import com.google.auth.oauth2.GoogleCredentials
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite

import java.time.temporal.ChronoUnit.DAYS
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Date
import scala.jdk.CollectionConverters.*

object CalendarServiceSpec extends SimpleIOSuite {
  private val now = Instant.parse("2024-06-24T20:00:00.000Z")
  private val timeMin = GoogleDateTime(
    Date.from(now.plus(1, DAYS).truncatedTo(DAYS))
  )
  private val zoneId = ZoneId.of("Europe/London")
  
  private val retrievedEvents =
    Events().setItems(
      List(
        Event()
          .setSummary("Event 1")
          .setStatus("Cancelled")
          .setId("1234")
          .setStart(EventDateTime().setDateTime(GoogleDateTime(Date.from(now)))),
        Event()
          .setSummary("Event 2")
          .setStatus("Active")
          .setId("5678")
          .setStart(EventDateTime().setDate(GoogleDateTime(Date.from(now.plus(1, DAYS))))),
        Event()
          .setSummary("Event 3")
          .setStatus("Cancelled")
          .setId("9101112")
          .setStart(EventDateTime().setDateTime(GoogleDateTime(Date.from(now.plus(3, DAYS))))),
        Event()
          .setSummary("Event 4")
          .setStatus("Active")
          .setId("13141516")
          .setStart(EventDateTime().setDate(GoogleDateTime(Date.from(now.plus(5, DAYS)))))
      ).asJava
    )
  private val expectedEventsSummary = List(
    EventSummary("Event 1", ZonedDateTime.ofInstant(now, zoneId)),
    EventSummary("Event 2", ZonedDateTime.ofInstant(now.plus(1, DAYS), zoneId)),
    EventSummary("Event 3", ZonedDateTime.ofInstant(now.plus(3, DAYS), zoneId)),
    EventSummary("Event 4", ZonedDateTime.ofInstant(now.plus(5, DAYS), zoneId))
  )

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val ownerEmail = "test@example.com"
  private val projectName = "life-aggregator-test"

  private def buildCalendarServiceStub(daysWindow: Int, retrievedEvents: Events): CalendarService[IO] =
    new CalendarService[IO] {
      override protected def executeRequest(request: Calendar#Events#List): IO[Events] = {
        IO.raiseUnless(
            request.getCalendarId == ownerEmail &&
              request.getSingleEvents &&
              request.getOrderBy == "starttime" &&
              request.getTimeMin == timeMin &&
              request.getTimeMax == GoogleDateTime(
                Date.from(
                  now
                    .plus(1, DAYS)
                    .truncatedTo(DAYS)
                    .plus(daysWindow, DAYS)
                )
              ) &&
              request.getTimeZone == "Europe/London"
          )(new RuntimeException("Incorrect request params"))
          .as(retrievedEvents)
      }
    }

  Seq(
    1,
    7
  ).foreach { daysWindow =>
    test(s"Should retrieve calendar events for the next $daysWindow days using access token") {
      val calendarServiceStub = buildCalendarServiceStub(
        daysWindow, retrievedEvents
      )

      for {
        result <- calendarServiceStub.retrieveEvents(
          new GoogleCredentials {}, projectName, ownerEmail, now, daysWindow
        )
      } yield expect(result == expectedEventsSummary)
    }
  }

  test("Should handle no calendar events gracefully") {
    val calendarServiceStub = buildCalendarServiceStub(
      daysWindow = 1, retrievedEvents = Events()
    )

    for {
      result <- calendarServiceStub.retrieveEvents(
        new GoogleCredentials {}, projectName, ownerEmail, now, 1
      )
    } yield expect(result.isEmpty)
  }
}
