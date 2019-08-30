import { Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'
import { TranslateService } from '@ngx-translate/core'
import { NzModalService } from 'ng-zorro-antd'
import { HomeService } from 'src/app/api/home.service'

@Component({
  selector: 'app-simulations',
  templateUrl: './simulations.component.html',
  styleUrls: ['./simulations.component.css']
})
export class SimulationsComponent implements OnInit {

  constructor(
    private homeService: HomeService,
    private modalService: NzModalService,
    private i18nService: TranslateService,
    private router: Router,
  ) { }

  run() {

  }

  ngOnInit(): void {
  }
}
