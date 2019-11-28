import { Component, OnInit } from '@angular/core'
import { NzDrawerService, NzMessageService, NzModalService } from 'ng-zorro-antd'
import { HomeService, WorkerData } from 'src/app/api/home.service'
import { getDefaultDrawerWidth } from 'src/app/util/drawer'

import { PeaMemberComponent } from '../shared/pea-member/pea-member.component'

@Component({
  selector: 'app-worker-peas',
  templateUrl: './worker-peas.component.html',
  styleUrls: ['./worker-peas.component.css']
})
export class WorkerPeasComponent implements OnInit {

  drawerWidth = getDefaultDrawerWidth()
  items: WorkerData[] = []

  constructor(
    private homeService: HomeService,
    private messageService: NzMessageService,
    private modalService: NzModalService,
    private drawerService: NzDrawerService,
  ) { }

  monitor(item: WorkerData) {
    this.drawerService.create({
      nzWidth: this.drawerWidth,
      nzContent: PeaMemberComponent,
      nzContentParams: {
        data: item
      },
      nzBodyStyle: {
        padding: '16px'
      },
      nzClosable: false,
    })
  }

  openResource(item: WorkerData) {
    const worker = item.member
    window.open(`http://${worker.address}:${worker.port}/resources`)
  }

  stop(item: WorkerData) {
    this.homeService.stopWorkers([item.member]).subscribe(res => {
      if (res.data.result) {
        this.loadData()
        this.messageService.success('OK')
      } else {
        this.modalService.create({
          nzTitle: 'Fail',
          nzContent: JSON.stringify(res.data.errors),
          nzClosable: true,
          nzOkDisabled: true,
        })
      }
    })
  }

  loadData() {
    this.homeService.getWorkers().subscribe(res => {
      this.items = res.data
    })
  }

  ngOnInit() {
    this.loadData()
  }
}
