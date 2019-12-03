import { CommonModule } from '@angular/common'
import { NgModule } from '@angular/core'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { TranslateModule } from '@ngx-translate/core'
import { NgZorroAntdModule } from 'ng-zorro-antd'

import { AssertionsComponent } from './assertions/assertions.component'
import { FeederComponent } from './feeder/feeder.component'
import { InjectionsBuilderComponent } from './injections-builder/injections-builder.component'
import { MemberSelectorComponent } from './member-selector/member-selector.component'
import { OshiInfoComponent } from './oshi-info/oshi-info.component'
import { PeaMemberComponent } from './pea-member/pea-member.component'
import { ResponseMonitorComponent } from './response-monitor/response-monitor.component'
import { ThrottleComponent } from './throttle/throttle.component'

const COMPONENTS = [
  PeaMemberComponent,
  InjectionsBuilderComponent,
  MemberSelectorComponent,
  OshiInfoComponent,
  ResponseMonitorComponent,
  FeederComponent,
  ThrottleComponent,
  AssertionsComponent,
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
