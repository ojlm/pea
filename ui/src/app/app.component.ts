import { Component } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  isCollapsed = false
  lang = 'en'

  changeLang() {
    const except = this.lang
    switch (this.lang) {
      case 'cn':
        this.lang = 'en'
        break
      case 'en':
        this.lang = 'cn'
        break
    }
    this.translate.use(except)
  }

  constructor(private translate: TranslateService) {
    // translate.setDefaultLang('en')
    translate.use('cn')
  }
}
