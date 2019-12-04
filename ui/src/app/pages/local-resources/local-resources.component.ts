import { Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'
import { TranslateService } from '@ngx-translate/core'
import { NzModalService, UploadChangeParam, UploadFile } from 'ng-zorro-antd'
import { ResourceService } from 'src/app/api/resource.service'
import { ResourceInfo } from 'src/app/model/pea.model'
import { formatFileSize } from 'src/app/util/file'

@Component({
  selector: 'app-local-resources',
  templateUrl: './local-resources.component.html',
  styleUrls: ['./local-resources.component.css']
})
export class LocalResourcesComponent implements OnInit {

  UPLOAD_BASE_URL = ''
  uploadUrl = ''
  fileList: UploadFile[] = []
  items: ResourceInfo[] = []
  breadcrumbItems: BreadcrumbPath[] = []
  path: string = ''
  newFlolderVisible = false
  newFolderName = ''
  isLibs = false

  constructor(
    private resourceService: ResourceService,
    private modalService: NzModalService,
    private i18nService: TranslateService,
    private router: Router,
  ) { }

  newFloder() {
    this.newFlolderVisible = true
  }

  handleOk() {
    this.resourceService.newFolder(this.path, this.newFolderName, this.isLibs).subscribe(res => {
      this.newFolderName = ''
      this.newFlolderVisible = false
      this.loadFiles()
    })
  }

  handleCancel() {
    this.newFolderName = ''
    this.newFlolderVisible = false
  }

  uploadChange(param: UploadChangeParam) {
    if (param.file.status === 'done') {
      const done = this.fileList.filter(file => file.status === 'done').length
      if (done === this.fileList.length) {
        this.fileList = []
        this.loadFiles()
      }
    }
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
    this.uploadUrl = `${this.UPLOAD_BASE_URL}?path=${this.path}`
    this.breadcrumbItems = tmp
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
      this.resourceService.read1k(`${this.path}/${item.filename}`, this.isLibs).subscribe(res => {
        this.modalService.create({
          nzTitle: `1K: ${item.filename}`,
          nzContent: `<pre>${res.data}</pre>`,
          nzClosable: true,
          nzOnOk: () => { }
        })
      })
    }
  }

  remove(item: ResourceInfo) {
    this.modalService.confirm({
      nzTitle: this.i18nService.instant('tips.deleteFile'),
      nzContent: item.filename,
      nzOnOk: () => {
        let path = item.filename
        if (this.path) {
          path = `${this.path}/${item.filename}`
        }
        this.resourceService.remove(path, this.isLibs).subscribe(res => {
          this.loadFiles()
        })
      }
    })
  }

  download(item: ResourceInfo) {
    let path = item.filename
    if (this.path) {
      path = `${this.path}/${item.filename}`
    }
    this.resourceService.download(path, this.isLibs)
  }

  downloadLink(item: ResourceInfo) {
    let path = item.filename
    if (this.path) {
      path = `${this.path}/${item.filename}`
    }
    return this.resourceService.downloadLink(path, this.isLibs)
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
    this.resourceService.list(this.path, this.isLibs).subscribe(res => {
      this.items = res.data
    })
  }

  ngOnInit() {
    this.isLibs = this.router.url === '/libs'
    if (this.isLibs) {
      this.UPLOAD_BASE_URL = '/api/resource/jar/upload'
    } else {
      this.UPLOAD_BASE_URL = '/api/resource/upload'
    }
    this.updateBreadcrumbItems()
    this.loadFiles()
  }
}

interface BreadcrumbPath {
  value: string
  path: string
}
