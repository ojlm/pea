import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { HomeService, WorkerData } from 'src/app/api/home.service'
import { PeaMember } from 'src/app/model/pea.model'

@Component({
  selector: 'app-member-selector',
  templateUrl: './member-selector.component.html',
  styleUrls: ['./member-selector.component.css']
})
export class MemberSelectorComponent implements OnInit, AfterViewInit {

  container: HTMLDivElement
  listStyle = {}
  titles = [
    this.i18nService.instant('tips.leftNodes'),
    this.i18nService.instant('tips.selectedNodes'),
  ]

  list: TransferMember[] = []
  _data: WorkerData[] = []

  @Input()
  set data(value: WorkerData[]) {
    if (value) this._data = value
  }
  get data() {
    return this._data
  }
  @Output()
  dataChange = new EventEmitter<WorkerData[]>()
  @HostListener('window:resize')
  resizeBy() {
    this.refreshTransferWidth()
  }

  constructor(
    private homeService: HomeService,
    private i18nService: TranslateService,
    private el: ElementRef,
  ) { }

  filterOption(term: string, item: PeaMember): boolean {
    return item.address.indexOf(term) > -1 || item.port.toString().indexOf(term) > -1 || item.hostname.indexOf(term) > -1
  }

  change(ret: {}): void {
    this._data = this.list.filter(item => item.direction === 'right').map(item => {
      return {
        member: {
          address: item.member.address,
          port: item.member.port,
          hostname: item.member.hostname
        },
        status: item.status
      }
    })
    this.dataChange.emit(this._data)
  }

  refreshTransferWidth() {
    if (this.container) {
      const width = (this.container.offsetWidth - 52) / 2
      this.listStyle = { 'width.px': width, 'height.px': 300 }
    }
  }

  ngOnInit(): void {
    this.homeService.getWorkers().subscribe(res => {
      this.list = res.data
    })
  }

  ngAfterViewInit(): void {
    const divs = (this.el.nativeElement as HTMLDivElement).getElementsByClassName('members-selector')
    if (divs && divs.length > 0) {
      this.container = divs[0] as HTMLDivElement
      this.refreshTransferWidth()
    }
  }
}

export interface TransferMember extends WorkerData {
  direction?: string
  checked?: boolean
  hide?: boolean
}
