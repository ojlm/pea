import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { WorkerPeasComponent } from './worker-peas.component'
import { WorkerPeasRoutingModule } from './worker-peas.routing.module'

@NgModule({
  imports: [
    SharedModule,
    WorkerPeasRoutingModule,
  ],
  declarations: [WorkerPeasComponent],
  exports: []
})
export class WorkerPeasModule { }
