import { AfterViewInit, Component, ElementRef, Input, OnDestroy } from '@angular/core'
import { NzMessageService } from 'ng-zorro-antd'
import { Subject } from 'rxjs'
import { GatlingService } from 'src/app/api/gatling.service'
import { WorkerData } from 'src/app/api/home.service'
import { ActorEvent, ActorEventType } from 'src/app/model/api.model'
import { ITerminalOptions, ITheme, Terminal } from 'xterm'
import { fit } from 'xterm/lib/addons/fit/fit'
import { webLinksInit } from 'xterm/lib/addons/webLinks/webLinks'

@Component({
  selector: 'app-compiler-output',
  templateUrl: './compiler-output.component.html',
  styleUrls: ['./compiler-output.component.css']
})
export class CompilerOutputComponent implements AfterViewInit, OnDestroy {

  addr = ''
  ws: WebSocket
  log: Subject<string> = new Subject()
  theme: ITheme = {
    foreground: 'lightslategray',
    background: 'white',
  }
  option: ITerminalOptions = {
    theme: this.theme,
    allowTransparency: true,
    cursorBlink: false,
    cursorStyle: 'block',
    fontFamily: 'monospace',
    fontSize: 12,
    disableStdin: true,
  }
  xterm = new Terminal(this.option)

  @Input()
  set data(data: WorkerData) {
    this.addr = `${data.member.address}:${data.member.port}`
    this.ws = this.gatlingService.compiler(data.member)
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

  constructor(
    private gatlingService: GatlingService,
    private msgService: NzMessageService,
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
