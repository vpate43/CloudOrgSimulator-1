package Simulations

import HelperUtils.{CreateLogger, ObtainConfigReference}
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerBestFit}
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts
import org.cloudbus.cloudsim.hosts.HostSimple
import org.cloudbus.cloudsim.resources.{Pe, PeSimple}
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms
import org.cloudbus.cloudsim.vms.VmSimple
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*

class CloudSimExperiment

object CloudSimExperiment:
  val config = ObtainConfigReference("cloudSimulator") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }
  val logger = CreateLogger(classOf[CloudSimExperiment])

  def Start() =
    val cloudsim = new CloudSim(); //Starting CloudSim
    val broker0 = new DatacenterBrokerBestFit(cloudsim); //Broker using a Best Fit mapping between cloudlets and Vm's

    //List of host PE's
    val hostPes = List(new PeSimple(config.getLong("cloudSimulator.host.mipsCapacity")))
    logger.info(s"Created one processing element: $hostPes")

    //List of hosts
    val hostList = List(new HostSimple(config.getLong("cloudSimulator.host.RAMInMBs"),
      config.getLong("cloudSimulator.host.StorageInMBs"),
      config.getLong("cloudSimulator.host.BandwidthInMBps"),
      hostPes.asJava))

    logger.info(s"Created one host: $hostList")

    val dc0 = new DatacenterSimple(cloudsim, hostList.asJava, new VmAllocationPolicySimple());
    //val dc1 = new DatacenterSimple(cloudsim, hostList.asJava);
    //val dc2 = new DatacenterSimple(cloudsim, hostList.asJava);


    val vmList = List(
      new VmSimple(config.getLong("cloudSimulator.vm.mipsCapacity"), hostPes.length)
        .setRam(config.getLong("cloudSimulator.vm.RAMInMBs"))
        .setBw(config.getLong("cloudSimulator.vm.BandwidthInMBps"))
        .setSize(config.getLong("cloudSimulator.vm.StorageInMBs"))
    )
    logger.info(s"Created one virtual machine: $vmList")

    val utilizationModel = new UtilizationModelDynamic(config.getDouble("cloudSimulator.utilizationRatio"));
    val cloudletList = new CloudletSimple(config.getLong("cloudSimulator.cloudlet.size"), config.getInt("cloudSimulator.cloudlet.PEs"), utilizationModel) ::
      new CloudletSimple(config.getLong("cloudSimulator.cloudlet.size"), config.getInt("cloudSimulator.cloudlet.PEs"), utilizationModel) :: Nil

    logger.info(s"Created a list of cloudlets: $cloudletList")

    broker0.submitVmList(vmList.asJava);
    broker0.submitCloudletList(cloudletList.asJava);

    logger.info("Starting cloud simulation...")
    cloudsim.start();

    //val finishedCloudlets = List(broker0.getCloudletFinishedList())
    new CloudletsTableBuilder(broker0.getCloudletFinishedList()).build();