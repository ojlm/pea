import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { JobSummaryComponent } from './job-summary/job-summary.component'
import { RunningJobComponent } from './running-job/running-job.component'
import { RunningJobsComponent } from './running-jobs.component'
import { RunningJobsRoutingModule } from './running-jobs.routing.module'
import { WorkerStatusComponent } from './worker-status/worker-status.component'

@NgModule({
  imports: [
    SharedModule,
    RunningJobsRoutingModule,
  ],
  declarations: [
    RunningJobsComponent,
    RunningJobComponent,
    JobSummaryComponent,
    WorkerStatusComponent,
  ],
  exports: []
})
export class RunningJobsModule { }
