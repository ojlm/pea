import { AfterViewInit, Component, ElementRef, Input, OnDestroy } from '@angular/core'
import { NzMessageService } from 'ng-zorro-antd'
import { Subject } from 'rxjs'
import { GatlingService } from 'src/app/api/gatling.service'
import { XtermService } from 'src/app/api/xterm.service'
import { ActorEvent, ActorEventType } from 'src/app/model/api.model'
import { PeaMember } from 'src/app/model/pea.model'
import { Terminal } from 'xterm'
import { fit } from 'xterm/lib/addons/fit/fit'
import { webLinksInit } from 'xterm/lib/addons/webLinks/webLinks'

@Component({
  selector: 'app-response-monitor',
  templateUrl: './response-monitor.component.html',
  styleUrls: ['./response-monitor.component.css']
})
export class ResponseMonitorComponent implements AfterViewInit, OnDestroy {

  ws: WebSocket
  log: Subject<string> = new Subject()
  xterm = new Terminal(this.xtermService.getDefaultOption())

  @Input()
  set data(data: PeaMember) {
    if (data) {
      this.ws = this.gatlingService.response(data)
      this.ws.onopen = (event) => { }
      this.ws.onmessage = (event) => {
        if (event.data) {
          try {
            const res = JSON.parse(event.data) as ActorEvent<string>
            if (ActorEventType.NOTIFY === res.type) {
              this.log.next(res.msg)
            }
          } catch (error) {
            this.msgService.error(error)
            this.ws.close()
          }
        }
      }
    }
  }

  constructor(
    private gatlingService: GatlingService,
    private msgService: NzMessageService,
    private xtermService: XtermService,
    private el: ElementRef<HTMLDivElement>,
  ) { }

  ngAfterViewInit(): void {
    const xtermEl = this.el.nativeElement.getElementsByClassName('xterm')[0] as HTMLElement
    this.xterm.open(xtermEl)
    fit(this.xterm)
    webLinksInit(this.xterm)
    this.log.subscribe(log => {
      this.xterm.writeln(log)
    })
  }

  ngOnDestroy(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
}
