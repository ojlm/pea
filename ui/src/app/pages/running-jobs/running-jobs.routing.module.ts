import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'

import { RunningJobComponent } from './running-job/running-job.component'
import { RunningJobsComponent } from './running-jobs.component'

const routes: Routes = [
  { path: '', component: RunningJobsComponent },
  { path: ':runId', component: RunningJobComponent },
]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RunningJobsRoutingModule { }
