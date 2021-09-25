//Author: Ronak Trivedi
//Purpose: Implement an PAAS cloud simulation
//Brokers have less control here as they cannot configure the datacenter or hosts
//  Essentially brokers are able to formulate a platform without specifying their infrastructure

package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import com.typesafe.config.{Config, ConfigFactory}
import Simulations.BasicCloudSimPlusExample.{config, logger}
import Simulations.PAAS_simulation.logger
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import org.cloudbus.cloudsim.cloudlets.Cloudlet
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.hosts.HostSimple
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.Vm
import org.cloudbus.cloudsim.schedulers.*
import org.cloudbus.cloudsim.vms.*
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicyBestFit}
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple

import collection.JavaConverters.*
import scala.collection.mutable.ListBuffer

class PAAS_simulation

object PAAS_simulation:
  //Obtaining the PAAS configuration for this simulation
  val config: Config = ConfigFactory.load("application.conf")
  val PAAS_config: Config = ConfigFactory.load("paas.conf")

  val logger = CreateLogger(classOf[PAAS_simulation])

  def Start() =
    val cloudsim = new CloudSim(); //Starting CloudSim

    val broker0 = new DatacenterBrokerSimple(cloudsim); //Broker using a simple mapping between cloudlets and Vm's
    logger.info(s"Created one broker: $broker0")

    //List of host PE's
    val hostPes = List(new PeSimple(config.getLong("IAAS_config.host.mipsCapacity")), new PeSimple(config.getLong("IAAS_config.host.mipsCapacity")), new PeSimple(config.getLong("IAAS_config.host.mipsCapacity")))
    logger.info(s"Created 3 processing elements: $hostPes")

    def createDatacenter(): Datacenter = {
      val hostList : ListBuffer[Host] = createHosts(); //Occupying list of hosts for the datacenter

      val dc0 = new DatacenterSimple(cloudsim, hostList.asJava, VmAllocationPolicySimple()) //Initialize datacenter
      val dc0_path:String = "PAAS_config.dc1."

      dc0.getCharacteristics //Configuring the datacenter, not hardcoded
        .setCostPerBw(PAAS_config.getDouble((dc0_path + "costPerBw")))
        .setCostPerMem(PAAS_config.getDouble((dc0_path + "costPerMem")))
        .setCostPerSecond(PAAS_config.getDouble((dc0_path + "costPerSecond")))
        .setCostPerStorage(PAAS_config.getDouble((dc0_path + "costPerStorage")))
        .setOs(PAAS_config.getString((dc0_path + "os")))

      dc0.setVmAllocationPolicy(new VmAllocationPolicySimple)

      return dc0
    }

    //Initializing list of hosts for the datacenter
    def createHosts(): ListBuffer[Host] ={
      val hostList = new ListBuffer[Host]
      (1 to config.getInt("IAAS_config.host.Num"))foreach (i=>
        hostList += (new HostSimple(config.getLong("IAAS_config.host.RAMInMBs"),config.getLong("IAAS_config.host.StorageInMBs"), config.getLong("IAAS_config.host.BandwidthInMBps"),hostPes.asJava)
          .setRamProvisioner(new ResourceProvisionerSimple())
          .setBwProvisioner(new ResourceProvisionerSimple())
          .setVmScheduler(new VmSchedulerTimeShared())
          )
        )

      logger.info(s"Created hosts: $hostList")
      return hostList
    }

    //Creating list of VM's for the hosts
    def createVM(): ListBuffer[Vm] ={
      val VMList = new ListBuffer[Vm]
      (1 to config.getInt("IAAS_config.VM.Num")) foreach(i =>
        VMList += new VmSimple(config.getLong("IAAS_config.VM.mipsCapacity"), hostPes.length)
          .setRam(config.getLong("IAAS_config.VM.RAMInMBs"))
          .setBw(config.getLong("IAAS_config.VM.BandwidthInMBps"))
          .setSize(config.getLong("IAAS_config.VM.StorageInMBs"))
          .setCloudletScheduler(new CloudletSchedulerSpaceShared)
        )

      logger.info(s"Created VM's: $VMList") //Logging info

      return VMList
    }

    //Initializing the utilization model
    val utilizationModel = new UtilizationModelDynamic(config.getDouble("IAAS_config.utilizationRatio"));

    //Creating cloudlets specified in config file
    def createCloudlet(): ListBuffer[Cloudlet] ={
      val cloudletList = new ListBuffer[Cloudlet]
      (1 to PAAS_config.getInt("PAAS_config.cloudlet.Num")) foreach(i =>
        cloudletList += CloudletSimple(PAAS_config.getLong("PAAS_config.cloudlet.size"), PAAS_config.getInt("PAAS_config.cloudlet.PEs"), utilizationModel)
        )

      logger.info(s"Created a list of cloudlets: $cloudletList") //Logging info

      return cloudletList
    }

    //Instantiate datacenters/hosts/VM's/cloudlets
    val datacenter0 = createDatacenter()
    val VM_list = createVM()
    val cloudlet_list = createCloudlet()

    //Submitting the VM's and cloudlets for the broker
    broker0.submitVmList(VM_list.asJava);
    broker0.submitCloudletList(cloudlet_list.asJava);

    logger.info("Starting cloud simulation...")
    cloudsim.start();

    //val finishedCloudlets = List(broker0.getCloudletFinishedList())
    new CloudletsTableBuilder(broker0.getCloudletFinishedList()).build();

    calculateCost(VM_list)

  //Calculating the cost of operation for the broker
  def calculateCost(vm_list:ListBuffer[Vm]): Unit = {
    val total_cost = new ListBuffer[Float]
    val processing_cost = new ListBuffer[Float]
    val memory_cost = new ListBuffer[Float]
    val storage_cost = new ListBuffer[Float]
    val bandwidth_cost = new ListBuffer[Float]
    for (VM <- vm_list) {
      val current_cost: VmCost = new VmCost(VM)
      total_cost += (current_cost.getTotalCost().toFloat);
      processing_cost += (current_cost.getProcessingCost().toFloat);
      memory_cost += (current_cost.getMemoryCost().toFloat);
      storage_cost += (current_cost.getStorageCost().toFloat);
      bandwidth_cost += (current_cost.getBwCost().toFloat);
    }

    //Outputting cost information
    logger.info("*******************************")
    logger.info("*******************************")
    logger.info("PAAS-COST REPORT:")
    logger.info("")
    logger.info(s"Total Cost: ${total_cost.sum}")
    logger.info(s"Processing Cost: ${processing_cost.sum}")
    logger.info(s"Bandwidth Cost: ${bandwidth_cost.sum}")
    logger.info("*******************************")
    logger.info("*******************************")
  }