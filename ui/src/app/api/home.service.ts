import { HttpClient } from '@angular/common/http'
import { Injectable } from '@angular/core'

import { ApiRes } from '../model/api.model'
import { PeaMember } from '../model/pea.model'
import { BaseService } from './base.service'

@Injectable({
  providedIn: 'root'
})
export class HomeService extends BaseService {

  constructor(private http: HttpClient) { super() }

  getRunningJobs() {
    return this.http.get<ApiRes<string[]>>(`${this.API_BASE}/jobs`)
  }

  getLocalReports() {
    return this.http.get<ApiRes<string[]>>(`${this.API_BASE}/reports`)
  }

  getWorkers() {
    return this.http.get<ApiRes<PeaMember[]>>(`${this.API_BASE}/workers`)
  }
}
