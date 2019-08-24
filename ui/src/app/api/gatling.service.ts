import { HttpClient } from '@angular/common/http'
import { Injectable } from '@angular/core'
import { NzMessageService } from 'ng-zorro-antd'

import { PeaMember } from '../model/pea.model'
import { newWS } from '../util/ws'
import { BaseService } from './base.service'

@Injectable({
  providedIn: 'root'
})
export class GatlingService extends BaseService {

  constructor(
    private http: HttpClient,
    private msgService: NzMessageService,
  ) { super() }

  monitor(member: PeaMember) {
    const ws = newWS(`${this.API_BASE}/gatling/monitor`, `${member.address}:${member.port}`)
    ws.onerror = (event) => {
      console.error(event)
      this.msgService.warning('Create Websocket error, see the console for more details.')
    }
    return ws
  }
}
