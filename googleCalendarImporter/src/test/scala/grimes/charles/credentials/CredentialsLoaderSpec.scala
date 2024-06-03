package grimes.charles.credentials

import cats.effect.IO
import cats.effect.kernel.Resource
import com.google.api.services.calendar.CalendarScopes.CALENDAR_EVENTS_READONLY
import com.google.auth.oauth2.GoogleCredentials
import io.circe.Json
import io.circe.parser.*
import org.apache.logging.log4j.{LogManager, Logger}
import org.http4s.Status.Ok
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIStringSyntax
import weaver.SimpleIOSuite

import java.io.InputStream
import java.util.UUID

object CredentialsLoaderSpec extends SimpleIOSuite {
  private val credentialsLoaderStub = new CredentialsLoader[IO] {
    override protected def buildCredsLoader(credsInputStream: InputStream, scopes: String*): IO[GoogleCredentials] =
      IO.raiseUnless(
        scopes == Seq("https://www.googleapis.com/auth/cloud-platform.read-only", CALENDAR_EVENTS_READONLY)
      )(new RuntimeException("Incorrect scopes"))
        .as(googleCredentialsStub)
  }

  private val googleCredentialsStub = new GoogleCredentials {
    override def refreshIfExpired(): Unit = ()
  }

  private given logger: Logger = LogManager.getLogger(this.getClass)
  private val awsSessionToken = UUID.randomUUID().toString
  private val serviceAccountCredsName = "my-service-account"  // Todo: Assert this gets used.
  private val serviceAccountCreds = parse("""
  {
    "type": "service_account",
    "project_id": "test",
    "private_key_id": "1234",
    "private_key": "MIIEvgIBADANB",
    "client_email": "test@google.com",
    "client_id": "12345",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test@google.com",
    "universe_domain": "googleapis.com"
  }
  """).getOrElse(Json.obj())
  private val expectedHeaders = Headers(
    Header.Raw(ci"X-Aws-Parameters-Secrets-Token", awsSessionToken),
    Header.Raw(ci"Accept", "text/*") 
  )
  private val expectedSSMUrl = "http://localhost:2773/systemsmanager/parameters/get?name=my-service-account"

  test("Should load access token from service account creds") {
    val stubClient = Client.apply[IO] { request =>
      if (request.headers == expectedHeaders &&
        request.uri.toString == expectedSSMUrl)
        Resource.eval(Ok(serviceAccountCreds.toString))
      else Resource.eval(BadRequest())
    }

    for {
      result <- credentialsLoaderStub.load(
        serviceAccountCredsName, awsSessionToken, stubClient
      ).attempt
    } yield expect(result.isRight)
  }
}