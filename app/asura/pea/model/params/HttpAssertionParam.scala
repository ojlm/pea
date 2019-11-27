package asura.pea.model.params

case class HttpAssertionParam(
                               status: AssertionsParam,
                               header: AssertionsParam,
                               body: AssertionsParam,
                             )
