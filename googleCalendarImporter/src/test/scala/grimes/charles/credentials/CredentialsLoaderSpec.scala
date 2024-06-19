package grimes.charles.credentials

import cats.effect.IO
import com.google.api.services.calendar.CalendarScopes.CALENDAR_EVENTS_READONLY
import com.google.auth.oauth2.GoogleCredentials
import grimes.charles.common.ssm.{Parameter, SSMResponse}
import org.http4s.dsl.io.*
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite

import java.io.InputStream

object CredentialsLoaderSpec extends SimpleIOSuite {
  private val credentialsLoaderStub = new CredentialsLoader[IO] {
    private val googleCredentialsStub = new GoogleCredentials {
      override def refreshIfExpired(): Unit = ()
    }
    
    override protected def buildCredsLoader(credsInputStream: InputStream, scopes: String*): IO[GoogleCredentials] =
      IO.raiseUnless(
        scopes == Seq("https://www.googleapis.com/auth/cloud-platform.read-only", CALENDAR_EVENTS_READONLY)
      )(new RuntimeException("Incorrect scopes"))
        .as(googleCredentialsStub)
  }

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  test("Should load access token from service account credentials") {
    val ssmParam = SSMResponse(Parameter("{\"type\":\"service_account\",\"project_id\":\"424114\",\"private_key_id\":\"12345\",\"private_key\":\"-----BEGIN PRIVATE KEY-----\\nObviouslyFake\\n-----END PRIVATE KEY-----\\n\",\"client_email\":\"test@example.gserviceaccount.com\",\"client_id\":\"4321\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_x509_cert_url\":\"https://www.googleapis.com/robot/v1/metadata/x509/test%40example.gserviceaccount.com\",\"universe_domain\":\"googleapis.com\"}"))

    for {
      result <- credentialsLoaderStub.load(ssmParam)
    } yield expect.all(
      result.getUniverseDomain == "googleapis.com",
      result.getAuthenticationType == "OAuth2"
    )
  }
}