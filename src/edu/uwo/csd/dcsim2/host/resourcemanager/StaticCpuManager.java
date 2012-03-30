package edu.uwo.csd.dcsim2.host.resourcemanager;

import edu.uwo.csd.dcsim2.vm.*;

public class StaticCpuManager extends CpuManager {
	
	double oversubscribeFactor = 1;
	
	public StaticCpuManager() {
		super();
	}
	
	public StaticCpuManager(double oversubscribeFactor) {
		this.oversubscribeFactor = oversubscribeFactor;
	}
	
	@Override
	public boolean isCapable(VMDescription vmDescription) {
		//check cores and core capacity
		if (vmDescription.getCores() * vmDescription.getCoreCapacity() > this.getTotalPhysicalCpu())
			return false;
		
		return true;
	}

	@Override
	public boolean hasCapacity(VMAllocationRequest vmAllocationRequest) {
		int requiredCapacity = 0;
		if (vmAllocationRequest.getCpuAllocation() != null) {
			requiredCapacity = vmAllocationRequest.getCpuAllocation().getTotalAlloc();
		} else {
			throw new RuntimeException("CPU hasCapacity request did not include a requested cpu allocation");
		}
		
		return requiredCapacity <= this.getAvailableAllocation();
	}

	@Override
	public void allocateResource(VMAllocationRequest vmAllocationRequest, VMAllocation vmAllocation) {

		if (hasCapacity(vmAllocationRequest)) {
			CpuAllocation newAlloc = new CpuAllocation();
			for (Integer coreCapacity : vmAllocationRequest.getCpuAllocation().getCoreAlloc()) {
				newAlloc.getCoreAlloc().add(coreCapacity);
			}
			vmAllocation.setCpuAllocation(newAlloc);
			allocationMap.put(vmAllocation, newAlloc);
		}
	}
	
	@Override
	public void allocatePrivDomain(VMAllocation privDomainAllocation) {

		if (this.getAvailableAllocation() >= 500) { //500 allows for 300 for the VMM and 200 for 2 migrations
			CpuAllocation newAlloc = new CpuAllocation(1, 500);
			privDomainAllocation.setCpuAllocation(newAlloc);
			setPrivDomainAllocation(privDomainAllocation);
		} else {
			throw new RuntimeException("Could not allocate privileged domain on Host #" + getHost().getId());
		}
	}

	@Override
	public void deallocateResource(VMAllocation vmAllocation) {
		vmAllocation.setCpuAllocation(null);
		allocationMap.remove(vmAllocation);
	}

	@Override
	public void updateAllocations() {
		//do nothing, allocation is static
	}

	@Override
	public int getTotalAllocationSize() {
		return (int)Math.round(this.getTotalPhysicalCpu() * oversubscribeFactor);
	}



}
