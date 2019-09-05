import { Component, OnInit } from '@angular/core'
import { HomeService, LocalReport } from 'src/app/api/home.service'

@Component({
  selector: 'app-local-reports',
  templateUrl: './local-reports.component.html',
  styleUrls: ['./local-reports.component.css']
})
export class LocalReportsComponent implements OnInit {

  items: LocalReport[] = []

  constructor(
    private homeService: HomeService,
  ) { }

  open(item: LocalReport) {
    window.open(`${location.protocol}//${location.host}/report/${item.name}`)
  }

  ngOnInit() {
    this.homeService.getLocalReports().subscribe(res => {
      this.items = res.data
    })
  }
}
