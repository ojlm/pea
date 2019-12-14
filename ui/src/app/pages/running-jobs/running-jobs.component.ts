import { Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'
import { HomeService } from 'src/app/api/home.service'
import { LoadTypes, ReporterJobStatus, StatusColors } from 'src/app/model/pea.model'

@Component({
  selector: 'app-running-jobs',
  templateUrl: './running-jobs.component.html',
  styleUrls: ['./running-jobs.component.css']
})
export class RunningJobsComponent implements OnInit {

  items: ReporterJobStatus[] = []

  constructor(
    private homeService: HomeService,
    private router: Router,
  ) { }

  open(item: ReporterJobStatus) {
    this.router.navigateByUrl(`/jobs/${item.runId}`)
  }

  statusColor(item: ReporterJobStatus) {
    return StatusColors[item.status]
  }

  itemColor(item: ReporterJobStatus) {
    if (item.load) {
      switch (item.load.type) {
        case LoadTypes.SINGLE:
          return 'magenta'
        case LoadTypes.SCRIPT:
          return 'cyan'
        case LoadTypes.PROGRAM:
          return 'blue'
      }
    } else {
      return ''
    }
  }

  itemDate(item: ReporterJobStatus) {
    return new Date(item.start).toLocaleString()
  }

  ngOnInit() {
    this.homeService.getRunningJobs().subscribe(res => {
      this.items = res.data
    })
  }
}
