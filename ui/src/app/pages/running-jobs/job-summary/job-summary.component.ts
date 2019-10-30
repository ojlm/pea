import { Component, Input } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { Injection, ReporterJobStatus, RunSimulationMessage, SingleHttpScenarioMessage } from 'src/app/model/pea.model'

@Component({
  selector: 'app-job-summary',
  templateUrl: './job-summary.component.html',
  styleUrls: ['./job-summary.component.css']
})
export class JobSummaryComponent {

  startTime = ''
  job: ReporterJobStatus = {}
  load: SingleHttpScenarioMessage & RunSimulationMessage
  @Input()
  set data(data: ReporterJobStatus) {
    if (data) {
      this.job = data
      this.startTime = new Date(data.start).toLocaleString()
      this.load = data.load
    }
  }

  constructor(
    private i18nService: TranslateService
  ) { }

  sumText(item: Injection) {
    let sum = `Users: ${item.users}`
    if (item.to) {
      sum = `${sum},${item.to}`
    }
    if (item.during && item.during.value && item.during.unit) {
      sum = `${sum}. During: ${item.during.value} ${this.i18nService.instant(`time.${item.during.unit}`)}.`
    }
    return sum
  }
}
