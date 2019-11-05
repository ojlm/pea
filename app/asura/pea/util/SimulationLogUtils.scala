package asura.pea.util

import java.io.File

import asura.pea.PeaConfig

object SimulationLogUtils {

  def simulationLogFile(runId: String): String = {
    s"${PeaConfig.resultsFolder}${File.separator}${runId}${File.separator}${PeaConfig.SIMULATION_LOG_FILE}"
  }
}
