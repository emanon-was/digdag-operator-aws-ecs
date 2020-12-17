package io.digdag.plugin.aws.ecs.runtask

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

case class EcsClientParams(
  val credentialsProvider: Option[AwsCredentialsProvider],
  val region: Option[Region]
)

object EcsClientParams {

  sealed trait Error extends Err
  object Error {
    case class UnexpectedErr(val err: Throwable) extends Error with Err.Throws
    case class ConfigJsonParseErr(val err: Throwable) extends Error with Err.Throws
    case class CredentialsParseErr(val err: Throwable) extends Error with Err.Throws
    case class ProfileParseErr(val err: Throwable) extends Error with Err.Throws
    case class RegionParseErr(val err: Throwable) extends Error with Err.Throws
    case class CredentialsProviderBuildErr(val err: Throwable) extends Error with Err.Throws
    case class RegionBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  def apply(config: Config): Either[Error, EcsClientParams] =
    for {
      json <- parse(config.toString()).left.map(Error.ConfigJsonParseErr)
      paramsJson <- json.flatten("aws.configure", "aws.ecs", "aws.ecs.run_task").toEither(Error.UnexpectedErr)
      credentialsParams <- paramsJson.as[ConfigParams.Credentials].left.map(Error.CredentialsParseErr)
      profileParams <- paramsJson.as[ConfigParams.Profile].left.map(Error.ProfileParseErr)
      regionParams <- paramsJson.as[ConfigParams.Region].left.map(Error.RegionParseErr)
      credentialsProvider <- credentialsProviderBuild(credentialsParams, profileParams).toEither(Error.CredentialsProviderBuildErr)
      region <- regionBuild(regionParams).toEither(Error.RegionBuildErr)
    } yield EcsClientParams(credentialsProvider, region)

  private def credentialsProviderBuild(credentials: ConfigParams.Credentials, profile: ConfigParams.Profile): Try[Option[AwsCredentialsProvider]] = {
    val result = (credentials.credentials, profile.profile) match {
      case (Some(a), _) => Some(staticCredentialsProvider(a))
      case (_, Some(b)) => Some(profileCredentialsProvider(b))
      case _ => None
    }
    result.sequence
  }

  private def staticCredentialsProvider(credentials: ConfigParams.CredentialsValue): Try[AwsCredentialsProvider] = Try {
    AwsBasicCredentials.create(credentials.access_key_id, credentials.secret_access_key).let(StaticCredentialsProvider.create(_))
  }

  private def profileCredentialsProvider(profile: ConfigParams.ProfileValue): Try[AwsCredentialsProvider] = Try {
    ProfileCredentialsProvider.builder()
      .also { builder => profile.name.map(builder.profileName(_)) }
      .also { builder => profile.file
               .map(Paths.get(_))
               .map(ProfileFile.builder().content(_).`type`(ProfileFile.Type.CREDENTIALS).build())
               .map(builder.profileFile(_)) }
      .build()
  }

  private def regionBuild(region: ConfigParams.Region): Try[Option[Region]] =
    Try(region.region.map(Region.of(_)))

}
