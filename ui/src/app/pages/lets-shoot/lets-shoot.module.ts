import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { LetsShootComponent } from './lets-shoot.component'
import { LetsShootRoutingModule } from './lets-shoot.routing.module'

@NgModule({
  imports: [
    SharedModule,
    LetsShootRoutingModule,
  ],
  declarations: [
    LetsShootComponent,
  ],
  exports: []
})
export class LetsShootModule { }
