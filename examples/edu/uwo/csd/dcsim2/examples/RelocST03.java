package edu.uwo.csd.dcsim2.examples;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uwo.csd.dcsim2.*;
import edu.uwo.csd.dcsim2.application.*;
import edu.uwo.csd.dcsim2.application.workload.*;
import edu.uwo.csd.dcsim2.core.*;
import edu.uwo.csd.dcsim2.host.*;
import edu.uwo.csd.dcsim2.host.model.*;
import edu.uwo.csd.dcsim2.host.power.*;
import edu.uwo.csd.dcsim2.host.resourcemanager.*;
import edu.uwo.csd.dcsim2.host.scheduler.*;
import edu.uwo.csd.dcsim2.management.*;
import edu.uwo.csd.dcsim2.vm.*;


public class RelocST03 {
	
	private static Logger logger = Logger.getLogger(RelocST03.class);
	
	private static int nHosts = 10;
	
	public static void main(String args[]) {
		
		PropertyConfigurator.configure(Simulation.getConfigDirectory() +"/logger.properties"); //configure logging from file

		logger.info(RelocST03.class.toString());
		
		//Set random seed to repeat run
		//Utility.setRandomSeed(-7376048803729825830l);
		
		//create datacentre
		VMPlacementPolicy vmPlacementPolicy = new VMPlacementPolicyFFD(); //new VMPlacementPolicyFixedCount(7);
		//VMPlacementPolicy vmPlacementPolicy = new VMPlacementPolicyFixedCount(7);
		DataCentre dc = new DataCentre(vmPlacementPolicy);
		
		Simulation.getInstance().setSimulationUpdateController(new DCSimUpdateController(dc));
		
		//create hosts
		ArrayList<Host> hostList = createHosts(nHosts);
		dc.addHosts(hostList);
		
		//create VMs
		int nVM = 6;
		
		ArrayList<VMAllocationRequest> vmList = new ArrayList<VMAllocationRequest>();
		for (int i = 0; i < nVM; ++i) {
			vmList.add(new VMAllocationRequest(createVMDesc("traces/clarknet", 1164, (int)(Utility.getRandom().nextDouble() * 200000000))));
		}
		for (int i = 0; i < nVM; ++i) {
			vmList.add(new VMAllocationRequest(createVMDesc("traces/epa", 975, (int)(Utility.getRandom().nextDouble() * 40000000))));
		}
		for (int i = 0; i < nVM; ++i) {
			vmList.add(new VMAllocationRequest(createVMDesc("traces/google_cores_job_type_0", 2271, (int)(Utility.getRandom().nextDouble() * 15000000))));
		}
		for (int i = 0; i < nVM; ++i) {
			vmList.add(new VMAllocationRequest(createVMDesc("traces/google_cores_job_type_1", 2325, (int)(Utility.getRandom().nextDouble() * 15000000))));
		}
		Collections.shuffle(vmList, Utility.getRandom());
		
		//submit VMs
		dc.getVMPlacementPolicy().submitVMs(vmList);
		
		//since this is a static run, turn off hosts with no VMs
		for (Host host : hostList) {
			if (host.getVMAllocations().size() == 0) {
				host.setState(Host.HostState.OFF);
			}
		}
		
		//create the VM relocation policy
		VMRelocationPolicy vmRelocationPolicy = new VMRelocationPolicyST03(dc, 600000, 0.5, 0.85, 0.85);
		//VMConsolidationPolicy vmConsolidationPolicy = new VMConsolidationPolicySimple(dc, 86400000, 0.5, 0.85);
		
		long startTime = System.currentTimeMillis();
		logger.info("Start time: " + startTime + "ms");
		
		//run the simulation
		//Simulation.getInstance().run(864000000, 86400000); //10 days, record metrics after 1 day
		//Simulation.getInstance().run(864000000, 0); //10 days
		Simulation.getInstance().run(86400000, 0); //1 day
		
		long endTime = System.currentTimeMillis();
		logger.info("End time: " + endTime + "ms. Elapsed: " + ((endTime - startTime) / 1000) + "s");
		
	}
	
	public static ArrayList<Host> createHosts(int nHosts) {
		
		ArrayList<Host> hosts = new ArrayList<Host>(nHosts);
		
		for (int i = 0; i < nHosts; ++i) {
			Host host = new ProLiantDL380G5QuadCoreHost(
					new StaticOversubscribingCpuManager(1500), //300 VMM overhead + 200 migration reserve
					new StaticMemoryManager(),
					new StaticBandwidthManager(131072), //assuming a separate 1Gb link for management!
					new StaticStorageManager(),
					new FairShareCpuScheduler());
			
			host.setHostPowerModel(new LinearHostPowerModel(250, 500)); //override default power model to match original DCSim experiments
			
			hosts.add(host);
		}
		
		return hosts;
	}
	
	public static VMDescription createVMDesc(String fileName, int coreCapacity, long offset) {
		
		//create workload (external)
		Workload workload = new TraceWorkload(fileName, 2700, offset); //scale of 2700 + 300 overhead = 1 core on ProLiantDL380G5QuadCoreHost
		
		//create single tier (web tier)
		WebServerTier webServerTier = new WebServerTier(1024, 0, 1, 0, 300); //256MB RAM, 0MG Storage, 1 cpu per request, 1 bw per request, 300 cpu overhead
		webServerTier.setWorkTarget(workload);
		
		//set the tier as the target for the external workload
		workload.setWorkTarget(webServerTier);
		
		//build VMDescription
		int cores = 1; //requires 1 core
		int memory = 1024;
		int bandwidth = 16384; //16MB = 16384KB
		long storage = 1024; //1GB
		VMDescription vmDescription = new VMDescription(cores, coreCapacity, memory, bandwidth, storage, webServerTier);

		return vmDescription;
	}
	
}
