import { Component, OnInit } from '@angular/core'
import { NzMessageService, NzModalService } from 'ng-zorro-antd'
import { HomeService, WorkerData } from 'src/app/api/home.service'
import { SimulationModel } from 'src/app/model/pea.model'

@Component({
  selector: 'app-simulations',
  templateUrl: './simulations.component.html',
  styleUrls: ['./simulations.component.css']
})
export class SimulationsComponent implements OnInit {

  workersAllChecked = false
  indeterminate = true

  lastCompileTime = ''
  simulation: SimulationModel = {}
  simulations: SimulationModel[] = []
  workers: SelectWorkerData[] = []
  compilers: SelectWorkerData[] = []

  constructor(
    private homeService: HomeService,
    private modalService: NzModalService,
    private messageService: NzMessageService,
  ) { }

  compile() {
    const workers = this.workers.filter(item => item.checked)
    if (workers.length > 0) {
      this.homeService.compile(workers.map(item => item.member)).subscribe(res => {
        if (res.data.result) {
          this.compilers = workers
        } else {
          this.modalService.create({
            nzTitle: 'Fail',
            nzContent: JSON.stringify(res.data.errors),
            nzClosable: true,
            nzOkDisabled: true,
          })
        }
      })
    } else {
      this.messageService.error('Empty nodes')
    }
  }

  updateAllChecked() {
    this.indeterminate = false
    if (this.workersAllChecked) {
      this.workers = this.workers.map(item => {
        return { ...item, checked: true }
      })
    } else {
      this.workers = this.workers.map(item => {
        return { ...item, checked: false }
      })
    }
  }

  updateSingleChecked() {
    if (this.workers.every(item => item.checked === false)) {
      this.workersAllChecked = false
      this.indeterminate = false
    } else if (this.workers.every(item => item.checked === true)) {
      this.workersAllChecked = true
      this.indeterminate = false
    } else {
      this.indeterminate = true
    }
  }

  ngOnInit(): void {
    this.homeService.getSimulations().subscribe(res => {
      this.lastCompileTime = new Date(res.data.last).toLocaleString()
      this.simulations = res.data.simulations
    })
    this.homeService.getWorkers().subscribe(res => {
      this.workers = res.data
    })
  }
}

interface SelectWorkerData extends WorkerData {
  checked?: boolean
}
