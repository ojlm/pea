import { Component, OnInit } from '@angular/core'
import { HomeService } from 'src/app/api/home.service'

@Component({
  selector: 'app-local-reports',
  templateUrl: './local-reports.component.html',
  styleUrls: ['./local-reports.component.css']
})
export class LocalReportsComponent implements OnInit {

  items: string[] = []

  constructor(
    private homeService: HomeService,
  ) { }

  open(item: string) {
    window.open(`${location.protocol}//${location.host}/report/${item}`)
  }

  ngOnInit() {
    this.homeService.getLocalReports().subscribe(res => {
      this.items = res.data
    })
  }
}
