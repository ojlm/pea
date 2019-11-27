import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { LocalResourcesComponent } from './local-resources.component'
import { LocalResourcesRoutingModule } from './local-resources.routing.module'

@NgModule({
  imports: [
    SharedModule,
    LocalResourcesRoutingModule,
  ],
  declarations: [
    LocalResourcesComponent,
  ],
  exports: []
})
export class LocalResourcesModule { }
