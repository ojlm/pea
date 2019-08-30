import { Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { HomeService } from 'src/app/api/home.service'
import { PeaMember, ReporterJobStatus } from 'src/app/model/pea.model'

@Component({
  selector: 'app-running-job',
  templateUrl: './running-job.component.html',
  styleUrls: ['./running-job.component.css']
})
export class RunningJobComponent implements OnInit, OnDestroy {

  ws: WebSocket
  runId = ''
  job: ReporterJobStatus = {}
  members: PeaMember[] = []
  startTime = ''

  constructor(
    private route: ActivatedRoute,
    private homeService: HomeService,
  ) { }

  loadJobDetails() {
    if (this.runId) {
      this.homeService.getJobDetails(this.runId).subscribe(res => {
        this.job = res.data
        this.startTime = new Date(res.data.start).toLocaleString()
        this.members = Object.keys(res.data.workers).map(key => {
          const addr = key.split(':')
          return { address: addr[0], port: parseInt(addr[1], 10) }
        })
      })
    }
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.runId = params['runId']
      this.loadJobDetails()
    })
  }

  ngOnDestroy(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
}
