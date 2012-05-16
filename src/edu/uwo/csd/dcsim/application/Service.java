package edu.uwo.csd.dcsim.application;

import java.util.*;

import edu.uwo.csd.dcsim.application.workload.*;
import edu.uwo.csd.dcsim.vm.*;

public class Service {

	private Workload workload;
	private ArrayList<ServiceTier> tiers;
	
	public Service() {
		tiers = new ArrayList<ServiceTier>();
	}
	
	public ArrayList<VMAllocationRequest> createInitialVmRequests() {
		ArrayList<VMAllocationRequest> vmList = new ArrayList<VMAllocationRequest>();
		for (ServiceTier tier : tiers) {
			for (int i = 0; i < tier.minSize; ++i)
				vmList.add(new VMAllocationRequest(tier.vmDescription));
		}
		return vmList;
	}
	
	/**
	 * Get the percentage of current incoming work to the Service for which SLA is violated
	 * @return
	 */
	public double getSLAViolation() {
		double sla = 0;
		for (ServiceTier tier : tiers) {
			for (Application app : tier.getApplications()) {
				sla += app.getSLAViolatedWork();
			}
		}
		
		sla = sla / workload.getCurrentWork();
		
		return sla;
	}
	
	/**
	 * Get the percentage of all incoming work to the Service for which SLA has been violated
	 * @return
	 */
	public double getTotalSLAViolation() {
		double sla = 0;
		for (ServiceTier tier : tiers) {
			for (Application app : tier.getApplications()) {
				sla += app.getTotalSLAViolatedWork();
			}
		}
		
		sla = sla / workload.getTotalWork();
		
		return sla;
	}
	
	public Workload getWorkload() {
		return workload;
	}
	
	public void setWorkload(Workload workload) {
		this.workload = workload;
	}
	
	public ArrayList<ServiceTier> getServiceTiers() {
		return tiers;
	}
	
	public void addServiceTier(ServiceTier serviceTier) {
		tiers.add(serviceTier);
	}
	
	public static class ServiceTier {
		
		private ApplicationTier applicationTier;
		private VMDescription vmDescription;
		private int minSize;
		private int maxSize;
		
		public ServiceTier(ApplicationTier applicationTier, VMDescription vmDescription) {
			this(applicationTier, vmDescription, 1, Integer.MAX_VALUE);
		}
		
		public ServiceTier(ApplicationTier applicationTier, VMDescription vmDescription, int minSize) {
			this(applicationTier, vmDescription, minSize, Integer.MAX_VALUE);
		}
		
		public ServiceTier(ApplicationTier applicationTier, VMDescription vmDescription, int minSize, int maxSize) {
			this.applicationTier = applicationTier;
			this.vmDescription = vmDescription;
			this.minSize = minSize;
			this.maxSize = maxSize;
		}
		
		public ArrayList<Application> getApplications() {
			return applicationTier.getApplications();
		}
		
		public VMDescription getVMDescription() {
			return vmDescription;
		}
		
		public ApplicationTier getApplicationTier() {
			return applicationTier;
		}
		
		public int getMinSize() {
			return minSize;
		}
		
		public int getMaxSize() {
			return maxSize;
		}
		
		public void setMinSize(int minSize) {
			this.minSize = minSize;
		}
		
		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}
		
	}
}
