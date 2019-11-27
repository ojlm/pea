import { Component, EventEmitter, Input, Output } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { SelectOption } from 'src/app/model/api.model'
import { Injection, InjectionType, TimeUnit } from 'src/app/model/pea.model'

@Component({
  selector: 'app-injections-builder',
  templateUrl: './injections-builder.component.html',
  styleUrls: ['./injections-builder.component.css']
})
export class InjectionsBuilderComponent {

  INJECTION_TYPES: SelectOption[] = Object.keys(InjectionType).map(k => {
    const v = InjectionType[k]
    return { label: this.i18nService.instant(`injection.${v}`), value: v }
  })
  TIME_UNITS: SelectOption[] = Object.keys(TimeUnit).map(k => {
    const v = TimeUnit[k]
    return { label: this.i18nService.instant(`time.${v}`), value: v }
  })
  injections: Injection[] = []
  current: Injection = {
    type: InjectionType.TYPE_AT_ONCE_USERS,
    users: 100,
    duration: {
      value: 1,
      unit: TimeUnit.TIME_UNIT_MINUTE,
    }
  }
  @Input()
  set data(value: Injection[]) {
    if (value) this.injections = value
  }
  get data() {
    return this.injections
  }
  @Output()
  dataChange = new EventEmitter<Injection[]>()

  constructor(
    private i18nService: TranslateService,
  ) { }

  add() {
    this.injections.push(this.copyInjection(this.current))
    this.injections = [...this.injections]
    this.dataChange.emit(this.data)
  }

  remove(item: Injection, index: number) {
    this.injections.splice(index, 1)
    this.injections = [...this.injections]
    this.dataChange.emit(this.data)
  }

  sumText(item: Injection) {
    let sum = `Users: ${item.users}`
    if (item.to) {
      sum = `${sum},${item.to}`
    }
    if (item.duration && item.duration.value && item.duration.unit) {
      sum = `${sum}. Duration: ${item.duration.value} ${this.i18nService.instant(`time.${item.duration.unit}`)}.`
    }
    return sum
  }

  copyInjection(injection: Injection): Injection {
    switch (injection.type) {
      case InjectionType.TYPE_RAMP_USERS:
        return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
      case InjectionType.TYPE_HEAVISIDE_USERS:
        return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
      case InjectionType.TYPE_AT_ONCE_USERS:
        return { type: injection.type, users: injection.users }
      case InjectionType.TYPE_CONSTANT_USERS_PER_SEC:
        return { type: injection.type, users: injection.users, duration: { ...injection.duration } }
      case InjectionType.TYPE_RAMP_USERS_PER_SEC:
        return { type: injection.type, users: injection.users, to: injection.to, duration: { ...injection.duration } }
    }
  }
}
