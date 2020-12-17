package io.digdag.plugin.aws.ecs.runtask

object ConfigParams {

  case class Credentials(
    val credentials: Option[CredentialsValue]
  )

  case class CredentialsValue(
    val access_key_id: String,
    val secret_access_key: String
  )

  case class Profile(
    val profile: Option[ProfileValue]
  )

  case class ProfileValue(
    val name: Option[String],
    val file: Option[String]
  )

  case class Region(
    val region: Option[String]
  )

}