import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core'
import { NzModalService } from 'ng-zorro-antd'
import { ResourceService } from 'src/app/api/resource.service'
import { FeederParam, ResourceInfo } from 'src/app/model/pea.model'
import { formatFileSize } from 'src/app/util/file'

@Component({
  selector: 'app-feeder',
  templateUrl: './feeder.component.html',
  styleUrls: ['./feeder.component.css']
})
export class FeederComponent implements OnInit {

  FEEDER_TYPES = ['csv', 'json']
  items: ResourceInfo[] = []
  breadcrumbItems: BreadcrumbPath[] = []
  path: string = ''
  checked: ResourceInfo = {}
  feeder: FeederParam = {}
  @Input()
  set data(value: FeederParam) {
    if (value) this.feeder = value
  }
  @Output()
  dataChange = new EventEmitter<FeederParam>()

  constructor(
    private resourceService: ResourceService,
    private modalService: NzModalService,
  ) { }

  modelChange() {
    this.dataChange.emit(this.feeder)
  }

  itemSize(item: ResourceInfo) {
    return formatFileSize(item.size)
  }

  itemColor(item: ResourceInfo) {
    if (item.isDirectory) {
      return '#1890ff'
    } else {
      return ''
    }
  }

  updateBreadcrumbItems() {
    const tmp: BreadcrumbPath[] = []
    this.path.split('/').forEach(value => {
      if (tmp.length === 0) {
        tmp.push({ value: value, path: value })
      } else {
        tmp.push({ value: value, path: `${tmp[tmp.length - 1].path}/${value}` })
      }
    })
    this.breadcrumbItems = tmp
  }

  select(item: ResourceInfo) {
    this.checked = item
    let path = item.filename
    if (this.path) {
      path = `${this.path}/${item.filename}`
    }
    this.feeder.path = path
    if (item.filename.endsWith('.json')) {
      this.feeder.type = 'json'
    } else {
      this.feeder.type = 'csv'
    }
    this.modelChange()
  }

  click(item: ResourceInfo) {
    if (item.isDirectory) {
      if (this.path) {
        this.path = `${this.path}/${item.filename}`
      } else {
        this.path = item.filename
      }
      this.updateBreadcrumbItems()
      this.loadFiles()
    } else {
      // read file
      this.resourceService.read1k(`${this.path}/${item.filename}`, false).subscribe(res => {
        this.modalService.create({
          nzTitle: `1K: ${item.filename}`,
          nzContent: `<pre>${res.data}</pre>`,
          nzClosable: true,
          nzOnOk: () => { }
        })
      })
    }
  }

  itemDate(item: ResourceInfo) {
    return new Date(item.modified).toLocaleString()
  }

  loadPath(path: string) {
    this.path = path
    this.updateBreadcrumbItems()
    this.loadFiles()
  }

  loadFiles() {
    this.resourceService.list(this.path, false).subscribe(res => {
      this.items = res.data
    })
  }

  ngOnInit() {
    this.updateBreadcrumbItems()
    this.loadFiles()
  }
}

interface BreadcrumbPath {
  value: string
  path: string
}
