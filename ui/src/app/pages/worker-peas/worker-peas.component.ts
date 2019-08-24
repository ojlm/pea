import { Component, OnInit } from '@angular/core'
import { NzDrawerService } from 'ng-zorro-antd'
import { HomeService } from 'src/app/api/home.service'
import { PeaMember } from 'src/app/model/pea.model'
import { getDefaultDrawerWidth } from 'src/app/util/drawer'

import { PeaMemberComponent } from '../shared/pea-member/pea-member.component'

@Component({
  selector: 'app-worker-peas',
  templateUrl: './worker-peas.component.html',
  styleUrls: ['./worker-peas.component.css']
})
export class WorkerPeasComponent implements OnInit {

  drawerWidth = getDefaultDrawerWidth()
  items: PeaMember[] = []

  constructor(
    private homeService: HomeService,
    private drawerService: NzDrawerService,
  ) { }

  monitor(item: PeaMember) {
    this.drawerService.create({
      nzWidth: this.drawerWidth,
      nzContent: PeaMemberComponent,
      nzContentParams: {
        data: item
      },
      nzBodyStyle: {
        padding: '4px'
      },
      nzClosable: false,
    })
  }

  ngOnInit() {
    this.homeService.getWorkers().subscribe(res => {
      this.items = res.data
    })
  }
}
