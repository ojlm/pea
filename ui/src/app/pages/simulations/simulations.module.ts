import { NgModule } from '@angular/core'

import { SharedModule } from '../shared/shared.module'
import { CompilerOutputComponent } from './compiler-output/compiler-output.component'
import { SimulationsComponent } from './simulations.component'
import { SimulationsRoutingModule } from './simulations.routing.module'

@NgModule({
  imports: [
    SharedModule,
    SimulationsRoutingModule,
  ],
  declarations: [
    SimulationsComponent,
    CompilerOutputComponent,
  ],
  exports: []
})
export class SimulationsModule { }
