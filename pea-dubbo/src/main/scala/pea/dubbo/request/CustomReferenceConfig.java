package pea.dubbo.request;

import org.apache.dubbo.config.ReferenceConfig;

public class CustomReferenceConfig<T> extends ReferenceConfig<T> {

  @Override
  public void checkAndUpdateSubConfigs() {
    // https://github.com/asura-pro/pea/issues/6
    ClassLoader reloadClassLoader = getInterfaceClass().getClassLoader();
    Thread.currentThread().setContextClassLoader(reloadClassLoader);
    super.checkAndUpdateSubConfigs();
  }
}
