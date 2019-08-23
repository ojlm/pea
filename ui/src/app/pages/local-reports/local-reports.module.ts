import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { LocalReportsComponent } from './local-reports.component'
import { LocalReportsRoutingModule } from './local-reports.routing.module'

@NgModule({
  imports: [
    SharedModule,
    LocalReportsRoutingModule,
  ],
  declarations: [
    LocalReportsComponent,
  ],
  exports: []
})
export class LocalReportsModule { }
