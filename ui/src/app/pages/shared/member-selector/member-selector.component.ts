import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { HomeService } from 'src/app/api/home.service'
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
  _data: PeaMember[] = []

  @Input()
  set data(value: PeaMember[]) {
    if (value) this._data = value
  }
  get data() {
    return this._data
  }
  @Output()
  dataChange = new EventEmitter<PeaMember[]>()
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
        address: item.address,
        port: item.port,
        hostname: item.hostname
      }
    })
    this.dataChange.emit(this._data)
  }

  refreshTransferWidth() {
    if (this.container) {
      const width = (this.container.offsetWidth - 48) / 2
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

export interface TransferMember extends PeaMember {
  direction?: string
  checked?: boolean
  hide?: boolean
}
