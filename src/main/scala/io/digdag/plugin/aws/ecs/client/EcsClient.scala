package io.digdag.plugin.aws.ecs.client

import scala.util.Try
import java.nio.file.Paths

import io.digdag.client.config.Config
import io.circe.parser.parse
import io.circe.generic.auto._
import io.digdag.plugin.aws.ecs.implicits._

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider,
  StaticCredentialsProvider,
  ProfileCredentialsProvider,
  AwsBasicCredentials,
}
import software.amazon.awssdk.profiles.ProfileFile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ecs.{
  EcsClient => SdkEcsClient
}

object EcsClient {

  sealed trait Error extends Err
  object Error {
    case class ConfigJsonParseErr(val err: Throwable) extends Error with Err.Throws
    case class CascadeJsonErr(val err: Throwable) extends Error with Err.Throws
    case class AwsClientConfigParseErr(val err: Throwable) extends Error with Err.Throws
    case class EcsClientBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  def apply(config: Config)(keys: String*): Either[Error, SdkEcsClient] =
    for {
      json <- parse(config.toString()).left.map(Error.ConfigJsonParseErr)
      json <- json.cascade(keys: _*).toEither(Error.CascadeJsonErr)
      config <- json.as[AwsClientConfig].left.map(Error.AwsClientConfigParseErr)
      client <- Try(config.build).toEither(Error.EcsClientBuildErr)
    } yield client


  implicit class EcsClientBuild(config: AwsClientConfig) {
    def build: SdkEcsClient = {
      val credentialsProvider = (config.credentials, config.profile) match {
        case (Some(a), _) => Some(a.build)
        case (_, Some(b)) => Some(b.build)
        case _ => None
      }

      SdkEcsClient.builder()
        .optional(credentialsProvider)((self, value) => self.credentialsProvider(value))
        .optional(config.region)((self, value) => self.region(Region.of(value)))
        .build()
    }
  }

  implicit class StaticCredentialsProviderBuild(credentials: AwsClientConfig.Credentials) {
    def build: AwsCredentialsProvider =
      AwsBasicCredentials.create(credentials.access_key_id, credentials.secret_access_key)
        .let(StaticCredentialsProvider.create(_))
  }

  implicit class ProfileCredentialsProviderBuild(profile: AwsClientConfig.Profile) {
    def build: AwsCredentialsProvider =
      ProfileCredentialsProvider.builder()
        .optional(profile.name)((self, value) => self.profileName(value))
        .optional(profile.file)((self, value) =>
          self.profileFile(
            ProfileFile.builder()
              .content(Paths.get(value))
              .`type`(ProfileFile.Type.CREDENTIALS)
              .build()))
        .build()
  }
}
