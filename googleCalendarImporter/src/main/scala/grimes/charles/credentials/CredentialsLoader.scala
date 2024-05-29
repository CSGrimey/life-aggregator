package grimes.charles.credentials

import cats.effect.IO

import java.io.ByteArrayInputStream
import com.google.auth.oauth2.GoogleCredentials
import com.google.api.services.calendar.CalendarScopes.*

import java.nio.charset.StandardCharsets.*

object CredentialsLoader {
  // Todo: Return resource.
  private def getServiceAccountCreds(serviceAccountCredsName: String, awsSessionToken: String): IO[String] = {
    // https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html
    val credentialsUrl = s"http://localhost:2773/systemsmanager/parameters/get/?name=$serviceAccountCredsName"

    //val req = urllib.request.Request(credentialsUrl)
    //req.add_header("X-Aws-Parameters-Secrets-Token", awsSessionToken)
    IO.pure("")
  }

  def load(serviceAccountCredsName: String, awsSessionToken: String): IO[GoogleCredentials] = 
    for {
      serviceAccountCredsString <- getServiceAccountCreds(serviceAccountCredsName, awsSessionToken)
      serviceAccountCredsInputStream = ByteArrayInputStream(serviceAccountCredsString.getBytes(UTF_8.name))

      googleCreds <- IO(
        GoogleCredentials
        .fromStream(serviceAccountCredsInputStream)
        .createScoped("https://www.googleapis.com/auth/cloud-platform", CALENDAR_EVENTS_READONLY)
      )
      _ <- IO.blocking(googleCreds.refreshIfExpired())
    } yield googleCreds
}