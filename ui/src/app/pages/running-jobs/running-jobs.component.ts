import { Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'
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
    private router: Router,
  ) { }

  open(item: string) {
    this.router.navigateByUrl(`/jobs/${item}`)
  }

  ngOnInit() {
    this.homeService.getRunningJobs().subscribe(res => {
      this.items = res.data
    })
  }
}
