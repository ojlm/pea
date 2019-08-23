import { NgModule } from '@angular/core'
import {
  DashboardOutline,
  DotChartOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  NumberOutline,
} from '@ant-design/icons-angular/icons'
import { NZ_ICONS } from 'ng-zorro-antd'

const icons = [MenuFoldOutline, MenuUnfoldOutline, DashboardOutline, DotChartOutline, NumberOutline,]

@NgModule({
  providers: [
    { provide: NZ_ICONS, useValue: icons }
  ]
})
export class IconsProviderModule {
}
