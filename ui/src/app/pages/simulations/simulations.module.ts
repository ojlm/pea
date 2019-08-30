import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { SimulationsComponent } from './simulations.component'
import { SimulationsRoutingModule } from './simulations.routing.module'

@NgModule({
  imports: [
    SharedModule,
    SimulationsRoutingModule,
  ],
  declarations: [
    SimulationsComponent,
  ],
  exports: []
})
export class SimulationsModule { }
