import { Component, Input } from '@angular/core'
import { JobWorkerStatus } from 'src/app/model/pea.model'

@Component({
  selector: 'app-worker-status',
  templateUrl: './worker-status.component.html',
  styleUrls: ['./worker-status.component.css']
})
export class WorkerStatusComponent {

  items: { worker: string, status: string }[] = []

  @Input()
  set data(data: { [k: string]: JobWorkerStatus }) {
    if (data) {
      this.items = Object.keys(data).map(k => {
        return { worker: k, status: data[k].status }
      })
    }
  }

  constructor() { }
}
