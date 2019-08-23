import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'

import { LetsShootComponent } from './lets-shoot.component'

const routes: Routes = [
  { path: '', component: LetsShootComponent },
]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LetsShootRoutingModule { }
