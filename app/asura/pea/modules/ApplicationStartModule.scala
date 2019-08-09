package asura.pea.modules

import asura.pea.hook.ApplicationStart
import com.google.inject.AbstractModule

class ApplicationStartModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }
}
