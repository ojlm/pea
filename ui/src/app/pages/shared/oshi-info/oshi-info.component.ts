import { Component, Input } from '@angular/core'
import { Oshi } from 'src/app/model/pea.model'

@Component({
  selector: 'app-oshi-info',
  templateUrl: './oshi-info.component.html',
  styleUrls: ['./oshi-info.component.css']
})
export class OshiInfoComponent {

  GB = 1024 * 1024 * 1024
  oshi: Oshi = {}

  @Input() label: string = ''
  @Input()
  set data(data: Oshi) {
    this.oshi = data || {}
  }

  formatMemorySize(size: number) {
    return (size / this.GB).toFixed(2)
  }

  constructor() { }
}
