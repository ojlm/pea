import { Component, Input } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { Injection, LoadJob, ReporterJobStatus, UnionLoadMessage } from 'src/app/model/pea.model'

@Component({
  selector: 'app-job-summary',
  templateUrl: './job-summary.component.html',
  styleUrls: ['./job-summary.component.css']
})
export class JobSummaryComponent {

  startTime = ''
  job: ReporterJobStatus = {}
  load: LoadJob & UnionLoadMessage
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
    if (item.duration && item.duration.value && item.duration.unit) {
      sum = `${sum}. Duration: ${item.duration.value} ${this.i18nService.instant(`time.${item.duration.unit}`)}.`
    }
    return sum
  }
}
