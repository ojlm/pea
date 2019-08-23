import { registerLocaleData } from '@angular/common'
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http'
import en from '@angular/common/locales/en'
import { NgModule } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { BrowserModule } from '@angular/platform-browser'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'
import { en_US, NgZorroAntdModule, NZ_I18N } from 'ng-zorro-antd'

import { ApiCodeInterceptor } from './api/api-code.interceptor'
import { AppRoutingModule } from './app-routing.module'
import { AppComponent } from './app.component'
import { IconsProviderModule } from './icons-provider.module'

registerLocaleData(en)

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    IconsProviderModule,
    NgZorroAntdModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
  ],
  providers: [
    { provide: NZ_I18N, useValue: en_US },
    { provide: HTTP_INTERCEPTORS, useClass: ApiCodeInterceptor, multi: true },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
