import { Component, Input, OnDestroy } from '@angular/core'
import { NzMessageService } from 'ng-zorro-antd'
import { GatlingService } from 'src/app/api/gatling.service'
import { ActorEvent, ActorEventType } from 'src/app/model/api.model'
import { MonitorData, PeaMember, PeaUserCounters, RequestCounters, TotalCounters } from 'src/app/model/pea.model'


@Component({
  selector: 'app-pea-member',
  templateUrl: './pea-member.component.html',
})
export class PeaMemberComponent implements OnDestroy {

  ws: WebSocket

  complete = false
  start = ''
  run = ''
  global: RequestCounters = {}
  total: TotalCounters = {}
  users: UserCountersItem[] = []
  requests: RequestCountersItem[] = []
  errors: ErrorsCountItem[] = []

  @Input()
  set data(member: PeaMember) {
    this.ws = this.gatlingService.monitor(member)
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

  constructor(
    private gatlingService: GatlingService,
    private msgService: NzMessageService,
  ) { }

  handleMonitorData(data: MonitorData) {
    console.log(data)
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
