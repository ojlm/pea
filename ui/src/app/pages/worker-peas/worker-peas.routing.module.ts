import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'

import { WorkerPeasComponent } from './worker-peas.component'

const routes: Routes = [
  { path: '', component: WorkerPeasComponent },
]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class WorkerPeasRoutingModule { }
