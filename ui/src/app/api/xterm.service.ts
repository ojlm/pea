import { Injectable } from '@angular/core'
import { ITerminalOptions, ITheme } from 'xterm'

import { BaseService } from './base.service'

@Injectable({
  providedIn: 'root'
})
export class XtermService extends BaseService {

  theme: ITheme = {
    foreground: 'lightslategray',
    background: 'white',
  }

  option: ITerminalOptions = {
    theme: this.theme,
    allowTransparency: true,
    cursorBlink: false,
    cursorStyle: 'block',
    fontFamily: 'monospace',
    fontSize: 12,
    disableStdin: true,
  }

  getDefaultTheme() {
    return this.theme
  }

  getDefaultOption() {
    return this.option
  }
}
