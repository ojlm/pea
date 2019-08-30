import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { RunningJobComponent } from './running-job/running-job.component'
import { RunningJobsComponent } from './running-jobs.component'
import { RunningJobsRoutingModule } from './running-jobs.routing.module'

@NgModule({
  imports: [
    SharedModule,
    RunningJobsRoutingModule,
  ],
  declarations: [
    RunningJobsComponent,
    RunningJobComponent,
  ],
  exports: []
})
export class RunningJobsModule { }
