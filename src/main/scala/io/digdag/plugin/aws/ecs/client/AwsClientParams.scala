package io.digdag.plugin.aws.ecs.client

import scala.util.Try
import cats.implicits._
import io.digdag.plugin.aws.ecs.implicits._
import java.nio.file.Paths
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

import io.digdag.client.config.Config
import io.circe.parser.parse
import io.circe.generic.auto._

case class AwsClientParams(
  val credentialsProvider: Option[AwsCredentialsProvider],
  val region: Option[Region]
)

object AwsClientParams {

  sealed trait Error extends Err
  object Error {
    case class ConfigJsonParseErr(val err: Throwable) extends Error with Err.Throws
    case class ConcatJsonErr(val err: Throwable) extends Error with Err.Throws
    case class AwsClientConfigParseErr(val err: Throwable) extends Error with Err.Throws
    case class CredentialsProviderBuildErr(val err: Throwable) extends Error with Err.Throws
    case class RegionBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  def apply(config: Config)(keys: String*): Either[Error, AwsClientParams] =
    for {
      json <- parse(config.toString()).left.map(Error.ConfigJsonParseErr)
      concatJson <- json.flatten(keys: _*).toEither(Error.ConcatJsonErr)
      config <- concatJson.as[AwsClientConfig].left.map(Error.AwsClientConfigParseErr)
      credentialsProvider <- credentialsProviderBuild(config.credentials, config.profile).toEither(Error.CredentialsProviderBuildErr)
      region <- regionBuild(config.region).toEither(Error.RegionBuildErr)
    } yield AwsClientParams(credentialsProvider, region)

  private def credentialsProviderBuild(credentials: Option[AwsClientConfig.Credentials], profile: Option[AwsClientConfig.Profile]): Try[Option[AwsCredentialsProvider]] = {
    val result = for {
      _ <- credentials.map(staticCredentialsProvider).toEither().swap
      _ <- profile.map(profileCredentialsProvider).toEither().swap
    } yield ()
    result.swap.toOption.sequence
  }

  private def staticCredentialsProvider(credentials: AwsClientConfig.Credentials): Try[AwsCredentialsProvider] = Try {
    AwsBasicCredentials.create(credentials.access_key_id, credentials.secret_access_key).let(StaticCredentialsProvider.create(_))
  }

  private def profileCredentialsProvider(profile: AwsClientConfig.Profile): Try[AwsCredentialsProvider] = Try {
    ProfileCredentialsProvider.builder()
      .also { builder => profile.name.map(builder.profileName(_)) }
      .also { builder => profile.file
               .map(Paths.get(_))
               .map(ProfileFile.builder().content(_).`type`(ProfileFile.Type.CREDENTIALS).build())
               .map(builder.profileFile(_)) }
      .build()
  }

  private def regionBuild(region: Option[String]): Try[Option[Region]] = Try {
    region.map(Region.of(_))
  }
}
