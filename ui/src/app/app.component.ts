import { Component, OnInit } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  isCollapsed = false
  lang = 'en'
  KEY_LANG = 'PEA_LANG'

  ngOnInit() {
    const lang = localStorage.getItem(this.KEY_LANG)
    if (lang === 'cn' || lang === 'en') {
      this.lang = lang
      this.changeLang()
    }
  }

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
    localStorage.setItem(this.KEY_LANG, except)
  }

  constructor(private translate: TranslateService) {
    // translate.setDefaultLang('en')
    translate.use('cn')
  }
}
