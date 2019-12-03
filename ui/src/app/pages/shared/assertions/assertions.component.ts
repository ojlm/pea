import { Component, EventEmitter, Input, Output } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { HttpAssertionParam } from 'src/app/model/pea.model'

@Component({
  selector: 'app-assertions',
  templateUrl: './assertions.component.html',
  styleUrls: ['./assertions.component.css']
})
export class AssertionsComponent {

  str = ``
  placeholder = `{
    "status" : {"list":[{"op":"eq", "except":200}]},
    "body" : {"list": [{"op":"jsonpath", "path":"$.msg", "except":"success"}]}
}`
  assertions: HttpAssertionParam = { status: { list: [] }, header: { list: [] }, body: { list: [] } }
  @Input()
  set data(value: HttpAssertionParam) {
    if (value) this.assertions = value
  }
  @Output()
  dataChange = new EventEmitter<HttpAssertionParam>()

  modelChange() {
    if (this.str) {
      try {
        this.assertions = JSON.parse(this.str) as HttpAssertionParam
        this.dataChange.emit(this.assertions)
      } catch (error) {
      }
    }
  }

  constructor(
    private i18nService: TranslateService,
  ) { }
}
