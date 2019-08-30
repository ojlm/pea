import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'

import { SimulationsComponent } from './simulations.component'

const routes: Routes = [
  { path: '', component: SimulationsComponent },
]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SimulationsRoutingModule { }
