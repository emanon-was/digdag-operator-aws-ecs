package io.digdag.plugin.aws.ecs

import io.digdag.spi.{OperatorFactory, Operator, OperatorContext, TaskResult}
import io.digdag.util.BaseOperator
import io.digdag.client.config.Config
import io.digdag.plugin.aws.ecs.implicits._
import org.slf4j.{Logger, LoggerFactory}

class RunTaskOperatorFactory(val operatorName: String) extends OperatorFactory {
  override def getType(): String = operatorName
  override def newOperator(ctx: OperatorContext): Operator = new RunTaskOperator(operatorName, ctx)
}

class RunTaskOperator(operatorName: String, ctx: OperatorContext) extends BaseOperator(ctx) {

  import io.digdag.plugin.aws.ecs.client._
  import io.digdag.plugin.aws.ecs.runtask._

  override def runTask(): TaskResult = {
    val config = request.getConfig
    val configFactory = config.getFactory()
    println(config)

    val result = for {
      ecsClient <- EcsClient(config)("aws.configure", "aws.ecs", "aws.ecs.run_task")

      // ecsResponse <- RunTask(ecsClient, operatorParams.params)
      // outputParams <- OutputParams(operatorParams.output, ecsResponse)(configFactory)
    } {

      println(ecsClient)
    }

    TaskResult.defaultBuilder(request).build()
    // val logger = LoggerFactory.getLogger(operatorName)
    // result match {
    //   case Left(err) => {
    //     logger.error("{}", err)
    //     err.panic
    //   }
    //   case Right(outputParams) => {
    //     logger.info("OutputParams: {}", outputParams)
    //     TaskResult.defaultBuilder(request)
    //       .also(_.storeParams(outputParams))
    //       .build()
    //   }
    // }
  }
}
