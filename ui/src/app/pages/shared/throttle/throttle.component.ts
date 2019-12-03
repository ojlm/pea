import { Component, EventEmitter, Input, Output } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { SelectOption } from 'src/app/model/api.model'
import { ThrottleParam, ThrottleStep, ThrottleTypes, TimeUnit } from 'src/app/model/pea.model'

@Component({
  selector: 'app-throttle',
  templateUrl: './throttle.component.html',
  styleUrls: ['./throttle.component.css']
})
export class ThrottleComponent {

  THROTTLE_TYPES: SelectOption[] = Object.keys(ThrottleTypes).map(k => {
    const v = ThrottleTypes[k]
    return { label: this.i18nService.instant(`throttle.${v}`), value: v }
  })
  TIME_UNITS: SelectOption[] = Object.keys(TimeUnit).map(k => {
    const v = TimeUnit[k]
    return { label: this.i18nService.instant(`time.${v}`), value: v }
  })
  throttle: ThrottleParam = { steps: [] }
  current: ThrottleStep = {
    type: ThrottleTypes.REACH,
    rps: 1000,
    duration: {
      value: 1,
      unit: TimeUnit.TIME_UNIT_MINUTE,
    }
  }
  @Input()
  set data(value: ThrottleParam) {
    if (value) this.throttle = value
  }
  @Output()
  dataChange = new EventEmitter<ThrottleParam>()

  constructor(
    private i18nService: TranslateService,
  ) { }

  add() {
    this.throttle.steps.push(this.copyStep(this.current))
    this.throttle.steps = [...this.throttle.steps]
    this.dataChange.emit(this.throttle)
  }

  remove(item: ThrottleStep, index: number) {
    this.throttle.steps.splice(index, 1)
    this.throttle.steps = [...this.throttle.steps]
    this.dataChange.emit(this.throttle)
  }

  sumText(item: ThrottleStep) {
    let sum = ''
    if (item.rps) {
      sum = `${sum} Rps: ${item.rps}`
    }
    if (item.duration && item.duration.value && item.duration.unit) {
      sum = `${sum} ${this.i18nService.instant('item.duration')}: ${item.duration.value} ${this.i18nService.instant(`time.${item.duration.unit}`)}.`
    }
    return sum
  }

  copyStep(step: ThrottleStep): ThrottleStep {
    switch (step.type) {
      case ThrottleTypes.REACH:
        return { type: step.type, rps: step.rps, duration: { ...step.duration } }
      case ThrottleTypes.HOLD:
        return { type: step.type, duration: { ...step.duration } }
      case ThrottleTypes.JUMP:
        return { type: step.type, rps: step.rps }
    }
  }
}
