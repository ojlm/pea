import { NgModule } from '@angular/core'
import {
  DashboardOutline,
  DesktopOutline,
  DotChartOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  NumberOutline,
  ScanOutline,
} from '@ant-design/icons-angular/icons'
import { NZ_ICONS } from 'ng-zorro-antd'

const icons = [
  MenuFoldOutline,
  MenuUnfoldOutline,
  DashboardOutline,
  DotChartOutline,
  NumberOutline,
  DesktopOutline,
  ScanOutline,
]

@NgModule({
  providers: [
    { provide: NZ_ICONS, useValue: icons }
  ]
})
export class IconsProviderModule {
}
