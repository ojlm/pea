import { Component, EventEmitter, Input, Output } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { SelectOption } from 'src/app/model/api.model'
import { ClosedInjectionType, Injection, OpenInjectionType, TimeUnit, WorkloadModels } from 'src/app/model/pea.model'
import {
  copyCleanInjection,
  injectionShowDuration,
  injectionShowEachLevelLasting,
  injectionShowFrom,
  injectionShowSeparatedByRampsLasting,
  injectionShowTimes,
  injectionShowTo,
  injectionShowUsers,
  injectionSumText,
} from 'src/app/util/injection'

@Component({
  selector: 'app-injections-builder',
  templateUrl: './injections-builder.component.html',
  styleUrls: ['./injections-builder.component.css']
})
export class InjectionsBuilderComponent {

  WORKLOAD_MODELS: SelectOption[] = Object.keys(WorkloadModels).map(k => {
    const v = WorkloadModels[k]
    return { label: this.i18nService.instant(`workload.${v}`), value: v }
  })
  INJECTION_TYPES: SelectOption[] = []
  TIME_UNITS: SelectOption[] = Object.keys(TimeUnit).map(k => {
    const v = TimeUnit[k]
    return { label: this.i18nService.instant(`time.${v}`), value: v }
  })
  workload = WorkloadModels.OPEN
  injections: Injection[] = []
  current: Injection = {
    type: OpenInjectionType.TYPE_AT_ONCE_USERS,
    users: 100,
    from: 0,
    to: 100,
    duration: {
      value: 1,
      unit: TimeUnit.TIME_UNIT_MINUTE,
    },
    times: 5,
    eachLevelLasting: {
      unit: TimeUnit.TIME_UNIT_MINUTE,
      value: 10,
    },
    separatedByRampsLasting: {
      unit: TimeUnit.TIME_UNIT_MINUTE,
    },
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
  ) {
    this.refreshInjectionTypes()
  }

  workloadModelChange() {
    this.refreshInjectionTypes()
    this.injections = []
    this.modelChange()
  }

  refreshInjectionTypes() {
    if (this.workload === WorkloadModels.OPEN) {
      this.current.type = OpenInjectionType.TYPE_AT_ONCE_USERS
      this.INJECTION_TYPES = Object.keys(OpenInjectionType).map(k => {
        const v = OpenInjectionType[k]
        return { label: this.i18nService.instant(`injection.${v}`), value: v }
      })
    } else if (this.workload === WorkloadModels.CLOSED) {
      this.current.type = ClosedInjectionType.TYPE_CONSTANT_CONCURRENT_USERS
      this.INJECTION_TYPES = Object.keys(ClosedInjectionType).map(k => {
        const v = ClosedInjectionType[k]
        return { label: this.i18nService.instant(`injection.${v}`), value: v }
      })
    }
  }

  modelChange() {
    this.dataChange.emit(this.data)
  }

  add() {
    this.injections.push(this.copyInjection(this.current))
    this.injections = [...this.injections]
    this.modelChange()
  }

  remove(item: Injection, index: number) {
    this.injections.splice(index, 1)
    this.injections = [...this.injections]
    this.modelChange()
  }

  sumText(item: Injection) {
    return injectionSumText(item, this.i18nService)
  }

  showUsers() {
    return injectionShowUsers(this.current)
  }

  showFrom() {
    return injectionShowFrom(this.current)
  }

  showTo() {
    return injectionShowTo(this.current)
  }

  showDuration() {
    return injectionShowDuration(this.current)
  }

  showTimes() {
    return injectionShowTimes(this.current)
  }

  showEachLevelLasting() {
    return injectionShowEachLevelLasting(this.current)
  }

  showSeparatedByRampsLasting() {
    return injectionShowSeparatedByRampsLasting(this.current)
  }

  copyInjection(injection: Injection): Injection {
    return copyCleanInjection(injection)
  }
}
