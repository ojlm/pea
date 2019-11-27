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

  list(file: string) {
    return this.http.post<ApiRes<ResourceInfo[]>>(`${this.API_BASE_RESOURCE}/list`, { file: file || '' })
  }

  remove(file: string) {
    return this.http.post<ApiRes<boolean>>(`${this.API_BASE_RESOURCE}/remove`, { file: file })
  }

  newFolder(path: string, name: string) {
    return this.http.put<ApiRes<boolean>>(`${this.API_BASE_RESOURCE}/folder`, { path: path, name: name })
  }
}
