package io.digdag.plugin.aws.ecs.client

case class AwsClientConfig(
  val credentials: Option[AwsClientConfig.Credentials],
  val profile: Option[AwsClientConfig.Profile],
  val region: Option[String]
)

object AwsClientConfig {
  case class Credentials(
    val access_key_id: String,
    val secret_access_key: String
  )
  case class Profile(
    val name: Option[String],
    val file: Option[String]
  )
}
