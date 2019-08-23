import { Component, OnInit } from '@angular/core'
import { HomeService } from 'src/app/api/home.service'
import { PeaMember } from 'src/app/model/pea.model'

@Component({
  selector: 'app-worker-peas',
  templateUrl: './worker-peas.component.html',
  styleUrls: ['./worker-peas.component.css']
})
export class WorkerPeasComponent implements OnInit {

  items: PeaMember[] = []

  constructor(
    private homeService: HomeService,
  ) { }

  ngOnInit() {
    this.homeService.getWorkers().subscribe(res => {
      this.items = res.data
    })
  }
}
