import { Component, OnInit } from '@angular/core'
import { NzMessageService, NzModalService } from 'ng-zorro-antd'
import { HomeService, WorkerData } from 'src/app/api/home.service'
import { MemberStatus, SimulationModel } from 'src/app/model/pea.model'

@Component({
  selector: 'app-simulations',
  templateUrl: './simulations.component.html',
  styleUrls: ['./simulations.component.css']
})
export class SimulationsComponent implements OnInit {

  workersAllChecked = false
  compilePull = false
  indeterminate = true

  lastCompileTime = ''
  editorBaseUrl = ''
  simulations: SimulationModel[] = []
  workers: SelectWorkerData[] = []
  compilers: SelectWorkerData[] = []

  constructor(
    private homeService: HomeService,
    private modalService: NzModalService,
    private messageService: NzMessageService,
  ) { }

  edit(simulation: SimulationModel) {
    if (this.editorBaseUrl) {
      const url = `${this.editorBaseUrl}${simulation.name.replace(/\./g, '/')}.scala`
      window.open(url)
    }
  }

  compile() {
    const workers = this.workers.filter(item => item.checked)
    if (workers.length > 0) {
      this.homeService.compile(workers.map(item => item.member), this.compilePull).subscribe(res => {
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

  loadSimulationsData() {
    this.homeService.getSimulations().subscribe(res => {
      this.lastCompileTime = new Date(res.data.last).toLocaleString()
      this.editorBaseUrl = res.data.editorBaseUrl
      this.simulations = res.data.simulations
    })
  }

  statusColor(item: SelectWorkerData) {
    if (MemberStatus.IDLE === item.status.status) {
      return 'lightseagreen'
    } else {
      return 'lightcoral'
    }
  }

  loadWorkersData() {
    this.homeService.getWorkers().subscribe(res => {
      this.workers = res.data
    })
  }

  ngOnInit(): void {
    this.loadSimulationsData()
    this.loadWorkersData()
  }
}

interface SelectWorkerData extends WorkerData {
  checked?: boolean
}
