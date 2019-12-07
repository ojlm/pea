package pea.app.dubbo.provider;

import pea.app.dubbo.api.GreetingService;

public class GreetingsServiceImpl implements GreetingService {

  @Override
  public String sayHello(String name) {
    return "hi, " + name;
  }
}
