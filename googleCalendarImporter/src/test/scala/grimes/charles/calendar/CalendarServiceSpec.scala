package grimes.charles.calendar

import cats.effect.IO
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Events
import com.google.api.services.calendar.model.Event
import com.google.auth.oauth2.GoogleCredentials
import org.apache.logging.log4j.{LogManager, Logger}
import weaver.SimpleIOSuite

import scala.jdk.CollectionConverters.*

object CalendarServiceSpec extends SimpleIOSuite {
  private val retrievedEvents = Events().setItems(
    List(
      Event().setSummary("Event 1"),
      Event().setSummary("Event 2"),
      Event().setSummary("Event 3")
    ).asJava
  )

  private val calendarServiceStub = new CalendarService[IO] {
    override protected def executeRequest(request: Calendar#Events#List): IO[Events] =
      IO.raiseUnless(
        request.getCalendarId == ownerEmail &&
        request.getSingleEvents &&
        request.getOrderBy == "starttime" &&
        // Todo: Assert timeMin and timeMax
        request.getTimeZone == "Europe/London" &&
        request.getMaxResults == 10
      )(new RuntimeException("Incorrect request params"))
        .as(retrievedEvents)
  }

  private given logger: Logger = LogManager.getLogger(this.getClass)

  private val ownerEmail = "test@example.com"
  private val projectName = "life-aggregator-test"

  test("Should retrieve calendar events using access token") {
    for {
      result <- calendarServiceStub.retrieveEvents(
        new GoogleCredentials {}, projectName, ownerEmail
      )
    } yield expect(result == retrievedEvents)
  }
}
