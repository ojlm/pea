package pea.app.model

import com.fasterxml.jackson.annotation.JsonProperty
import oshi.SystemInfo

case class OshiInfo(
                     os: String,
                     @JsonProperty("memory.total")
                     memoryTotal: Long,
                     @JsonProperty("memory.available")
                     memoryAvailable: Long,
                     @JsonProperty("cpu.name")
                     cpuName: String,
                     @JsonProperty("cpu.physical.processor.count")
                     cpuPhysicalProcessorCount: Int,
                     @JsonProperty("cpu.logical.processor.count")
                     cpuLogicalProcessorCount: Int,
                   )

object OshiInfo {

  def getOshiInfo(): OshiInfo = {
    val si = new SystemInfo()
    val os = si.getOperatingSystem
    val hardware = si.getHardware
    val memory = hardware.getMemory
    val cpu = hardware.getProcessor
    OshiInfo(
      os = os.toString,
      memoryTotal = memory.getTotal,
      memoryAvailable = memory.getAvailable,
      cpuName = cpu.getName,
      cpuPhysicalProcessorCount = cpu.getPhysicalProcessorCount,
      cpuLogicalProcessorCount = cpu.getLogicalProcessorCount,
    )
  }
}
