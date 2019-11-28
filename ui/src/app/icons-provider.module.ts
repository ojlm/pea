import { NgModule } from '@angular/core'
import {
  CodeOutline,
  DashboardOutline,
  DeleteOutline,
  DesktopOutline,
  DotChartOutline,
  FileOutline,
  FolderOpenOutline,
  FolderOutline,
  HomeOutline,
  InfoCircleOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  NumberOutline,
  PlusOutline,
  RadarChartOutline,
  RiseOutline,
  ScanOutline,
  ScheduleOutline,
  StopOutline,
  SyncOutline,
  UploadOutline,
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
  StopOutline,
  ScheduleOutline,
  RadarChartOutline,
  SyncOutline,
  InfoCircleOutline,
  FolderOutline,
  FileOutline,
  DeleteOutline,
  HomeOutline,
  UploadOutline,
  PlusOutline,
  FolderOpenOutline,
]

@NgModule({
  providers: [
    { provide: NZ_ICONS, useValue: icons }
  ]
})
export class IconsProviderModule {
}
