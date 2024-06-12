package grimes.charles.calendar

import cats.effect.IO
import com.google.api.client.util.*
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.{Event, EventDateTime, Events}
import com.google.auth.oauth2.GoogleCredentials
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite

import java.time.temporal.ChronoUnit.DAYS
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Date
import scala.jdk.CollectionConverters.*

object CalendarServiceSpec extends SimpleIOSuite {
  private val utc = ZoneId.of("UTC")
  private val now = LocalDateTime.of(2024, 6, 24, 9, 0).toInstant(ZoneOffset.UTC)
  private val timeMin = DateTime(Date.from(now))
  
  private val retrievedEvents =
    Events().setItems(
      List(
        Event()
          .setSummary("Event 1")
          .setStatus("Cancelled")
          .setId("1234")
          .setStart(EventDateTime().setDateTime(DateTime(Date.from(now)))),
        Event()
          .setSummary("Event 2")
          .setStatus("Active")
          .setId("5678")
          .setStart(EventDateTime().setDateTime(DateTime(Date.from(now.plus(2, DAYS))))),
        Event()
          .setSummary("Event 3")
          .setStatus("Cancelled")
          .setId("9101112")
          .setStart(EventDateTime().setDateTime(DateTime(Date.from(now.plus(5, DAYS)))))
      ).asJava
    )
  private val expectedEventsSummary = List(
    EventSummary("Event 1", ZonedDateTime.ofInstant(now, utc)),
    EventSummary("Event 2", ZonedDateTime.ofInstant(now.plus(2, DAYS), utc)),
    EventSummary("Event 3", ZonedDateTime.ofInstant(now.plus(5, DAYS), utc)),
  )

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val ownerEmail = "test@example.com"
  private val projectName = "life-aggregator-test"

  private def buildCalendarServiceStub(daysWindow: Int, retrievedEvents: Events): CalendarService[IO] =
    new CalendarService[IO] {
      override protected def executeRequest(request: Calendar#Events#List): IO[Events] =
        IO.raiseUnless(
            request.getCalendarId == ownerEmail &&
              request.getSingleEvents &&
              request.getOrderBy == "starttime" &&
              request.getTimeMin == timeMin &&
              request.getTimeMax == DateTime(Date.from(now.plus(daysWindow, DAYS))) &&
              request.getTimeZone == "Europe/London"
          )(new RuntimeException("Incorrect request params"))
          .as(retrievedEvents)
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

  test(s"Should handle no calendar events gracefully") {
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
