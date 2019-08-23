import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http'
import { Injectable, Injector, isDevMode } from '@angular/core'
import { NzMessageService } from 'ng-zorro-antd'
import { Observable, of } from 'rxjs'
import { mergeMap } from 'rxjs/operators'

import { APICODE, ApiResObj } from '../model/api.model'

@Injectable()
export class ApiCodeInterceptor implements HttpInterceptor {
  message: NzMessageService
  constructor(private inj: Injector) {
    this.message = this.inj.get(NzMessageService)
  }
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.url.startsWith('./assets')) {
      return next.handle(req)
    } else {
      return next.handle(req).pipe(mergeMap((event: HttpEvent<any>, i: number) => {
        // tslint:disable-next-line:no-bitwise
        if (event instanceof HttpResponse && ~(event.status / 100) < 3) {
          const res = event as HttpResponse<ApiResObj>
          if (isDevMode()) {
            // console.log(req.url, res.body)
          }
          const code = res.body.code
          if (APICODE.NOT_LOGIN === code) {
          } else if (APICODE.OK === code) {
            return of(event)
          } else {
            const errMsg = res.body.msg
            this.message.error(errMsg)
          }
        } else {
          return of(event)
        }
      }))
    }
  }
}
