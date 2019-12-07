package pea.app.modules

import com.google.inject.AbstractModule
import pea.app.hook.ApplicationStart

class ApplicationStartModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }
}
