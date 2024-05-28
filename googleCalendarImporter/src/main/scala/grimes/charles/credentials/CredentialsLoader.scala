package grimes.charles.credentials

import cats.effect.IO
import java.io.ByteArrayInputStream
import com.google.auth.oauth2.GoogleCredentials
import com.google.api.services.calendar.CalendarScopes.*
import java.nio.charset.StandardCharsets.*

object CredentialsLoader {
  // Todo: Return resource.
  private def getServiceAccountCreds(serviceAccountCredsName: String, awsSessionToken: String): String = {
    // https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html
    val credentialsUrl = s"http://localhost:2773/systemsmanager/parameters/get/?name=$serviceAccountCredsName"

    //val req = urllib.request.Request(credentialsUrl)
    //req.add_header("X-Aws-Parameters-Secrets-Token", awsSessionToken)
    ""
  }

  def load(serviceAccountCredsName: String, awsSessionToken: String): IO[GoogleCredentials] = {
    val serviceAccountCredsString = getServiceAccountCreds(serviceAccountCredsName, awsSessionToken)
    val credsStream = ByteArrayInputStream(serviceAccountCredsString.getBytes(UTF_8.name))
    val credentials = GoogleCredentials
      .fromStream(credsStream)
      .createScoped("https://www.googleapis.com/auth/cloud-platform", CALENDAR_EVENTS_READONLY)
    credentials.refreshIfExpired()
    IO.pure(credentials)
  }
}