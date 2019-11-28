import { Component, Input } from '@angular/core'
import { ReporterJobStatus } from 'src/app/model/pea.model'

@Component({
  selector: 'app-job-summary',
  templateUrl: './job-summary.component.html',
  styleUrls: ['./job-summary.component.css']
})
export class JobSummaryComponent {

  startTime = ''
  job: ReporterJobStatus = {}
  @Input()
  set data(data: ReporterJobStatus) {
    if (data) {
      this.job = data
      this.startTime = new Date(this.job.start).toLocaleString()
    }
  }

  constructor() { }
}
