package io.digdag.plugin.aws.ecs

import java.util.{Arrays => JArrays, List => JList}
import io.digdag.spi.{Plugin, OperatorProvider, OperatorFactory}

class AwsECSPlugin extends Plugin {
  override def getServiceProvider[T](clazz: Class[T]): Class[_ <: T] = {
    if (clazz != classOf[OperatorProvider]) null
    else classOf[AwsECSOperatorProvider].asSubclass(clazz)
  }
}

class AwsECSOperatorProvider extends OperatorProvider {
  override def get(): JList[OperatorFactory] = JArrays.asList(
    new RunTaskOperatorFactory("aws.ecs.run_task"),
  )
}
