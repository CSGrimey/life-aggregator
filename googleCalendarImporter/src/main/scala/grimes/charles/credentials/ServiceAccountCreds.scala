package grimes.charles.credentials

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor}

object ServiceAccountCreds {
  given decoder: Decoder[ServiceAccountCreds] = deriveDecoder[ServiceAccountCreds]
  given encoder: Encoder[ServiceAccountCreds] = deriveEncoder[ServiceAccountCreds]
}

// Todo: Improve types? Also do we need every field?
case class ServiceAccountCreds(
                                `type`: String, 
                                project_id: String, 
                                private_key_id: String, 
                                private_key: String, 
                                client_email: String, 
                                client_id: String, 
                                auth_uri: String, 
                                token_uri: String, 
                                auth_provider_x509_cert_url: String, 
                                client_x509_cert_url: String, 
                                universe_domain: String
                              )