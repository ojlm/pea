import { CommonModule } from '@angular/common'
import { NgModule } from '@angular/core'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { TranslateModule } from '@ngx-translate/core'
import { NgZorroAntdModule } from 'ng-zorro-antd'

import { PeaMemberComponent } from './pea-member/pea-member.component'

const COMPONENTS = [
  PeaMemberComponent,
]

const ENTRY_COMPONENTS = [
  PeaMemberComponent,
]

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ReactiveFormsModule,
    TranslateModule,
    NgZorroAntdModule,
  ],
  declarations: [
    ...COMPONENTS,
  ],
  entryComponents: [
    ...ENTRY_COMPONENTS,
  ],
  exports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ReactiveFormsModule,
    TranslateModule,
    NgZorroAntdModule,
    ...COMPONENTS,
  ]
})
export class SharedModule { }
