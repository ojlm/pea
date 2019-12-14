import { Component, Input, OnDestroy } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { NzMessageService } from 'ng-zorro-antd'
import { GatlingService } from 'src/app/api/gatling.service'
import { WorkerData } from 'src/app/api/home.service'
import { ActorEvent, ActorEventType } from 'src/app/model/api.model'
import {
  Injection,
  JobWorkerStatus,
  LoadTypes,
  MonitorData,
  PeaMember,
  PeaUserCounters,
  RequestCounters,
  TotalCounters,
  UnionLoadMessage,
} from 'src/app/model/pea.model'
import { injectionSumText } from 'src/app/util/injection'

@Component({
  selector: 'app-pea-member',
  templateUrl: './pea-member.component.html',
  styleUrls: ['./pea-member.component.css']
})
export class PeaMemberComponent implements OnDestroy {

  ws: WebSocket

  consoleVisible = false
  complete = false
  start = ''
  run = ''
  member: PeaMember
  status: JobWorkerStatus = {}
  global: RequestCounters = {}
  total: TotalCounters = {}
  users: UserCountersItem[] = []
  requests: RequestCountersItem[] = []
  errors: ErrorsCountItem[] = []
  request: UnionLoadMessage

  @Input()
  set data(data: WorkerData) {
    this.request = data.request
    this.status = data.status
    this.member = data.member
    if (this.request) {
      if (LoadTypes.PROGRAM === this.request.type) {
        this.showConsole()
      } else {
        this.startGatlingMonitor()
      }
    } else {
      this.startGatlingMonitor()
    }
  }

  constructor(
    private gatlingService: GatlingService,
    private msgService: NzMessageService,
    private i18nService: TranslateService,
  ) { }

  startGatlingMonitor() {
    this.ws = this.gatlingService.monitor(this.member)
    this.ws.onopen = (event) => { }
    this.ws.onmessage = (event) => {
      if (event.data) {
        try {
          const res = JSON.parse(event.data) as ActorEvent<MonitorData>
          if (ActorEventType.ITEM === res.type) {
            this.handleMonitorData(res.data)
          }
        } catch (error) {
          this.msgService.error(error)
          this.ws.close()
        }
      }
    }
  }

  memberAddr() {
    if (this.member) {
      return `${this.member.address}:${this.member.port}`
    } else {
      return ''
    }
  }

  showConsole() {
    this.consoleVisible = true
  }

  handleMonitorData(data: MonitorData) {
    this.complete = data.complete
    this.start = new Date(data.start).toLocaleString()
    this.run = `${data.run} seconds`
    this.global = data.global
    this.total = data.total
    this.users = Object.keys(data.users).map(k => {
      return { name: k, counters: data.users[k] }
    })
    this.requests = Object.keys(data.requests).map(k => {
      return { name: k, counters: data.requests[k] }
    })
    this.errors = Object.keys(data.errors).map(k => {
      return { name: k, count: data.errors[k] }
    })
  }

  sumText(item: Injection) {
    return injectionSumText(item, this.i18nService)
  }

  ngOnDestroy(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
}

export interface UserCountersItem {
  name: string
  counters: PeaUserCounters
}

export interface RequestCountersItem {
  name: string
  counters: RequestCounters
}

export interface ErrorsCountItem {
  name: string
  count: number
}
