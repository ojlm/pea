import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { RunningJobsComponent } from './running-jobs.component'
import { RunningJobsRoutingModule } from './running-jobs.routing.module'

@NgModule({
  imports: [
    SharedModule,
    RunningJobsRoutingModule,
  ],
  declarations: [
    RunningJobsComponent,
  ],
  exports: []
})
export class RunningJobsModule { }
