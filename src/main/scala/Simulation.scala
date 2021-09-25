import HelperUtils.{CreateLogger, ObtainConfigReference}
import Simulations.{BasicCloudSimPlusExample, IAAS_simulation, PAAS_simulation, SAAS_simulation}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Simulation:
  val logger = CreateLogger(classOf[Simulation])

  @main def runSimulation =
    logger.info("Constructing an IAAS cloud model...")
    IAAS_simulation.Start()
    logger.info("Finished IAAS cloud simulation...")
    logger.info("Constructing a PAAS cloud model...")
    PAAS_simulation.Start()
    logger.info("Finished PAAS cloud simulation...")
    logger.info("Constructing a SAAS cloud model...")
    SAAS_simulation.Start()
    logger.info("Finished SAAS cloud simulation...")

class Simulation