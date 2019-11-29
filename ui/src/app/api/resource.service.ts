import { HttpClient } from '@angular/common/http'
import { Injectable } from '@angular/core'

import { ApiRes } from '../model/api.model'
import { ResourceInfo } from '../model/pea.model'
import { BaseService } from './base.service'

@Injectable({
  providedIn: 'root'
})
export class ResourceService extends BaseService {

  API_BASE_RESOURCE = `${this.API_BASE}/resource`
  constructor(private http: HttpClient) { super() }

  read1k(path: string, isLibs: boolean) {
    return this.http.get<ApiRes<string>>(`${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/read1k?path=${path}`)
  }

  list(file: string, isLibs: boolean) {
    return this.http.post<ApiRes<ResourceInfo[]>>(`${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/list`, { file: file || '' })
  }

  remove(file: string, isLibs: boolean) {
    return this.http.post<ApiRes<boolean>>(`${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/remove`, { file: file })
  }

  newFolder(path: string, name: string, isLibs: boolean) {
    return this.http.put<ApiRes<boolean>>(`${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/folder`, { path: path, name: name })
  }

  getDownloadLink(isLibs: boolean) {
    return `${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/download`
  }

  download(path: string, isLibs: boolean) {
    const url = `${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/download?path=${path}`
    this.http.get<Blob>(url, { responseType: 'blob' as 'json' }).subscribe(res => {
      const link = window.URL.createObjectURL(res)
      window.open(link)
    })
  }

  downloadLink(path: string, isLibs: boolean) {
    return `${this.API_BASE_RESOURCE}${isLibs ? '/jar' : ''}/download?path=${path}`
  }
}
