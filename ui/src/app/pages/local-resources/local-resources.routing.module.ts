import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'

import { LocalResourcesComponent } from './local-resources.component'

const routes: Routes = [
  { path: '', component: LocalResourcesComponent },
]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LocalResourcesRoutingModule { }
