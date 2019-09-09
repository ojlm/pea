package asura.pea.dubbo.provider;

import asura.pea.dubbo.api.GreetingService;

public class GreetingsServiceImpl implements GreetingService {

  @Override
  public String sayHello(String name) {
    return "hi, " + name;
  }
}
