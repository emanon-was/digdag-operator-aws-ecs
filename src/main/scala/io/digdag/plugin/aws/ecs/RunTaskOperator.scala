package io.digdag.plugin.aws.ecs

import scala.util.Try
import collection.JavaConverters._

import io.digdag.spi.{OperatorFactory, Operator, OperatorContext, TaskResult}
import io.digdag.util.BaseOperator
import io.digdag.client.config.Config
import io.digdag.client.config.ConfigKey
import io.digdag.plugin.aws.ecs.implicits._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.services.ecs.model.Task

class RunTaskOperatorFactory(val operatorName: String) extends OperatorFactory {
  override def getType(): String = operatorName
  override def newOperator(ctx: OperatorContext): Operator = new RunTaskOperator(operatorName, ctx)
}

class RunTaskOperator(operatorName: String, ctx: OperatorContext) extends BaseOperator(ctx) {

  import io.digdag.plugin.aws.ecs.client._
  import io.digdag.plugin.aws.ecs.runtask._
  import io.digdag.plugin.aws.ecs.wait._

  sealed trait Error extends Err
  object Error {
    case class RunTaskErr(val err: Throwable) extends Error with Err.Throws
  }

  override def runTask(): TaskResult = {
    val config = request.getConfig
    val configFactory = config.getFactory()
    println(config)

    val result = for {
      ecsClient <- EcsClient(config)("aws.configure", "aws.ecs", "aws.ecs.run_task")
      runTaskRequest <- RunTaskRequest(config)("aws.ecs.run_task")
      response <- Try(ecsClient.runTask(runTaskRequest)).toEither(Error.RunTaskErr)
    } yield response

    val logger = LoggerFactory.getLogger(operatorName)
    result match {
      case Left(err) => {
        logger.error("{}", err)
        err.panic
      }
      case Right(response) => {
        logger.info("Response: {}", response)
        val result = WaitConfig(
          WaitConfig.RunTask(
            response.tasks().asScala.toList.map(task =>
              WaitConfig.Task(
                task.clusterArn(),
                task.taskArn(),
                task.taskDefinitionArn())
            )))
        TaskResult.defaultBuilder(request)
          .resetStoreParams(List(ConfigKey.of("last_run_task")).asJava)
          .also(_.storeParams(configFactory.fromJsonString(result.asJson.noSpaces)))
          .build()
      }
    }
  }
}
