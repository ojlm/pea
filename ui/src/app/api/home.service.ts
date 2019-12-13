import { HttpClient } from '@angular/common/http'
import { Injectable } from '@angular/core'

import { ApiRes } from '../model/api.model'
import {
  JobWorkerStatus,
  PeaMember,
  ReporterJobStatus,
  RunScriptJob,
  Simulations,
  SingleHttpScenarioJob,
  UnionLoadMessage,
  WorkersAvailable,
} from '../model/pea.model'
import { BaseService } from './base.service'

@Injectable({
  providedIn: 'root'
})
export class HomeService extends BaseService {

  constructor(private http: HttpClient) { super() }

  getRunningJobs() {
    return this.http.get<ApiRes<ReporterJobStatus[]>>(`${this.API_BASE}/jobs`)
  }

  getJobDetails(runId: string) {
    return this.http.get<ApiRes<ReporterJobStatus>>(`${this.API_BASE}/job/${runId}`)
  }

  getLocalReports() {
    return this.http.get<ApiRes<LocalReport[]>>(`${this.API_BASE}/reports`)
  }

  getWorkers() {
    return this.http.get<ApiRes<WorkerData[]>>(`${this.API_BASE}/workers`)
  }

  stopWorkers(workers: PeaMember[]) {
    return this.http.post<ApiRes<WorkersBoolResponse>>(`${this.API_BASE}/stop`, { workers: workers })
  }

  compile(workers: PeaMember[], pull: boolean) {
    return this.http.post<ApiRes<WorkersBoolResponse>>(`${this.API_BASE}/compile`, { workers: workers, pull: pull })
  }

  getSimulations() {
    return this.http.get<ApiRes<Simulations>>(`${this.API_BASE}/simulations`)
  }

  runSingleHttpScenarioJob(load: SingleHttpScenarioJob) {
    return this.http.post<ApiRes<WorkersAvailable>>(`${this.API_BASE}/single`, load)
  }

  runScriptJob(load: RunScriptJob) {
    return this.http.post<ApiRes<WorkersAvailable>>(`${this.API_BASE}/script`, load)
  }

}

export interface WorkerData {
  member?: PeaMember
  status?: JobWorkerStatus
  request?: UnionLoadMessage
}

export interface WorkersBoolResponse {
  result: boolean
  errors: object
}

export interface LocalReport {
  name?: string
  last?: string
}
