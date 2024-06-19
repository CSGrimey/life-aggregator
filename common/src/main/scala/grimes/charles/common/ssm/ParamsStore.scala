package grimes.charles.common.ssm

import cats.effect.Async
import org.http4s.Method.*
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.*
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.typelevel.ci.CIStringSyntax

class ParamsStore[F[_] : Async] {
  // https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html
  private val ssmGetParamUrl = uri"http://localhost:2773/systemsmanager/parameters/get"

  def get(paramName: String, awsSessionToken: String, client: Client[F]): F[SSMResponse] = {
    val request = Request[F](
      method = GET,
      uri = ssmGetParamUrl
        .withQueryParam("name", paramName)
        .withQueryParam("withDecryption", true),
      headers = Headers(
        Header.Raw(ci"X-Aws-Parameters-Secrets-Token", awsSessionToken)
      )
    )

    client.expect[SSMResponse](request)(jsonOf[F, SSMResponse])
  }
}