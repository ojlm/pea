import { Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { HomeService, WorkerData } from 'src/app/api/home.service'
import { ReporterJobStatus } from 'src/app/model/pea.model'

@Component({
  selector: 'app-running-job',
  templateUrl: './running-job.component.html',
  styleUrls: ['./running-job.component.css']
})
export class RunningJobComponent implements OnInit, OnDestroy {

  runId = ''
  job: ReporterJobStatus = {}
  members: WorkerData[] = []
  constructor(
    private route: ActivatedRoute,
    private homeService: HomeService,
  ) { }

  loadJobDetails() {
    if (this.runId) {
      this.homeService.getJobDetails(this.runId).subscribe(res => {
        this.job = res.data
        const load = res.data.load
        let tmp: WorkerData[] = []
        if (load) {
          if (load.jobs && load.jobs.length > 0) {
            load.jobs.forEach(item => {
              tmp.push({ member: item.worker, request: item.load })
            })
          } else if (load.load && load.workers && load.workers.length > 0) {
            load.workers.forEach(item => {
              tmp.push({ member: item, request: load.load })
            })
          }
        }
        this.members = tmp
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
  }
}
