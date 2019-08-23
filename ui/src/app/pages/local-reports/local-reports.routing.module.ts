import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'

import { LocalReportsComponent } from './local-reports.component'

const routes: Routes = [
  { path: '', component: LocalReportsComponent },
]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LocalReportsRoutingModule { }
