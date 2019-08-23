import { Component, OnInit } from '@angular/core'
import { HomeService } from 'src/app/api/home.service'

@Component({
  selector: 'app-running-jobs',
  templateUrl: './running-jobs.component.html',
  styleUrls: ['./running-jobs.component.css']
})
export class RunningJobsComponent implements OnInit {

  items: string[] = []

  constructor(
    private homeService: HomeService,
  ) { }

  open(item: string) {

  }

  ngOnInit() {
    this.homeService.getRunningJobs().subscribe(res => {
      this.items = res.data
    })
  }
}
