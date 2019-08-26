import { NgModule } from '@angular/core'
import {
  CodeOutline,
  DashboardOutline,
  DeleteOutline,
  DesktopOutline,
  DotChartOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  NumberOutline,
  RiseOutline,
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
  DeleteOutline,
  RiseOutline,
  CodeOutline,
]

@NgModule({
  providers: [
    { provide: NZ_ICONS, useValue: icons }
  ]
})
export class IconsProviderModule {
}
