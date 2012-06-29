/*
 * Title:        DCSim Toolkit
 * Description:  DCSim (Data Centre Simulator) Toolkit for Modeling and Simulation of Data Centres
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */
package edu.uwo.csd.dcsim.management;

import java.util.ArrayList;
import java.util.Collections;

import edu.uwo.csd.dcsim.DataCentre;
import edu.uwo.csd.dcsim.core.Simulation;
import edu.uwo.csd.dcsim.management.stub.HostStub;
import edu.uwo.csd.dcsim.management.stub.HostStubCpuInUseComparator;
import edu.uwo.csd.dcsim.management.stub.HostStubPowerStateComparator;
import edu.uwo.csd.dcsim.management.stub.VmStub;
import edu.uwo.csd.dcsim.management.stub.VmStubCpuInUseComparator;

/**
 * VmRelocationPolicyFFDM implements the following VM relocation policy:
 * 
 * - relocation candidates: VMs with less CPU load than the CPU load by which 
 *   the host is stressed are ignored. The rest of the VMs are sorted in 
 *   decreasing order by CPU load;
 * - target hosts: sort Partially-utilized hosts in increasing order by CPU 
 *   utilization, Underutilized hosts in decreasing order by CPU utilization, 
 *   and Empty hosts in decreasing order by power state. Return the hosts in 
 *   the following order: Partially-utilized, Underutilized, and Empty.
 * 
 * @author gkeller2
 *
 */
public class VMRelocationPolicyFFDM extends VMRelocationPolicyGreedy {

	/**
	 * Creates a new instance of VMRelocationPolicyFFDM.
	 */
	public VMRelocationPolicyFFDM(Simulation simulation, DataCentre dc, long interval, double lowerThreshold, double upperThreshold, double targetUtilization) {
		super(simulation, dc, interval, lowerThreshold, upperThreshold, targetUtilization);
	}
	
	/**
	 * Sorts the relocation candidates in decreasing order by CPU load, 
	 * previously removing from consideration those VMs with less CPU load 
	 * than the CPU load by which the host is stressed.
	 */
	@Override
	protected ArrayList<VmStub> orderSourceVms(ArrayList<VmStub> sourceVms) {
		//ArrayList<VmStub> sorted = new ArrayList<VmStub>(sourceVms);
		ArrayList<VmStub> sorted = new ArrayList<VmStub>();
		
		// Remove VMs with less CPU load than the CPU load by which the source 
		// host is stressed.
		
		// DO WE REALLY WANT TO DO THIS OPTIMIZATION WHEN SORTING VMs IN DECREASING ORDER ???
		
		HostStub source = sourceVms.get(0).getHost();
		double cpuExcess = source.getCpuInUse() - source.getTotalCpu() * this.upperThreshold;
		for (VmStub vm : sourceVms)
			if (vm.getCpuInUse() >= cpuExcess)
				sorted.add(vm);
		if (sorted.isEmpty())
			sorted.addAll(sourceVms);
		
		// Sort VMs in decreasing order by CPU load.
		Collections.sort(sorted, new VmStubCpuInUseComparator());		// Is this comparator using an absolute value of CPU (load) instead of a relative value (utilization %) ???
		Collections.reverse(sorted);
		
		return sorted;
	}
	
	/**
	 * Sorts Partially-utilized hosts in increasing order by CPU utilization, 
	 * Underutilized hosts in decreasing order by CPU utilization, and Empty 
	 * hosts in decreasing order by power state. 
	 * Returns the target hosts in the following order: Partially-utilized, 
	 * Underutilized, and Empty.
	 */
	@Override
	protected ArrayList<HostStub> orderTargetHosts(
			ArrayList<HostStub> partiallyUtilized,
			ArrayList<HostStub> underUtilized, ArrayList<HostStub> empty) {
		
		ArrayList<HostStub> targets = new ArrayList<HostStub>();
		
		// Sort Partially-utilized hosts in increasing order by CPU utilization.
		Collections.sort(partiallyUtilized, new HostStubCpuInUseComparator());
		//Collections.reverse(partiallyUtilized);
		
		// Sort Underutilized hosts in decreasing order by CPU utilization.
		Collections.sort(underUtilized, new HostStubCpuInUseComparator());
		Collections.reverse(underUtilized);
		
		// Sort Empty hosts in decreasing order by power state 
		// (on, suspended, off).
		Collections.sort(empty, new HostStubPowerStateComparator());
		Collections.reverse(empty);
		
		targets.addAll(partiallyUtilized);
		targets.addAll(underUtilized);
		targets.addAll(empty);
		
		return targets;
	}

}
