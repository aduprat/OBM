import com.excilys.ebi.gatling.core.util.PathHelper.path2string
import com.excilys.ebi.gatling.recorder.config.Options
import com.excilys.ebi.gatling.recorder.controller.RecorderController

import IDEPathHelper.{ requestBodiesDirectoryPath, recorderOutputDirectoryPath }

object Recorder extends App {

	RecorderController(Options(
		outputFolder = Some(recorderOutputDirectoryPath),
		simulationPackage = Some("org.obm.opush"),
		requestBodiesFolder = Some(requestBodiesDirectoryPath)))
}