package org.cloudbus.cloudsim.examples;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;



public class vehicularBroker extends DatacenterBroker{
	
	/** The cloudlet new arrival list. */
	protected List<VTasks> cloudletNewList = new ArrayList<VTasks>();
	protected List<VTasks> cloudletList = new ArrayList<VTasks>();
	
	
	//create sending cloudlet event
	public final static int CLOUDLET_SUBMIT_RESUME = 125;
	//exchange completion time between datacenter and broker	
	public final static int ESTIMATED_COMPLETION_TIME = 126;
	//create period event
	public final static int PERIODIC_EVENT = 127;
	//task Id
	public static int taskId = 1;
	
	public List<Vm> vmDestroyedList = new ArrayList<Vm>(); // using existing vm. But need to create vehicular vm
	
	
	double periodicDelay = 5; //contains the delay to the next periodic event
    boolean generatePeriodicEvent = true; //true if new internal events have to be generated
    
    public Map<Integer, Double> totalCompletionTime_vmMap = new HashMap<Integer, Double>();
    
    
    int vmIndex;
    int temp_key = 0;
    
    
    int cloudletSubmittedCount;
    boolean broker_vm_deallocation_flag =false;
    
	static int testBrokerCount=0;

	
   
    //set up DatacenterCharacteristics;
	public DatacenterCharacteristics characteristics;
	public boolean startupqueue;
	public String sortalgorithm;
	
    //flag = 0, cloudlet is from batch queue
    //flag = 1, cloudlet is from new arrival queue
    private int switch_flag = 0;
    
    
	
	//constructor for vehicular broker
	public vehicularBroker(String name, DatacenterCharacteristics characteristics) throws Exception {
        super(name);
   	

   }
	
	/**
	 * Processes events available for this Broker.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);
				break;
			// VM Creation answer
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev);
				break;
			// A finished cloudlet returned
			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;
			// if the simulation finishes
			case CloudSimTags.END_OF_SIMULATION:
				shutdownEntity();
				break;
			/**
			* @override
			* add a new tag CLOUDLET_SUBMIT_RESUME to broker
			* updating cloudletwaitinglist in VideoSegmentScheduler, whenever a vm's waitinglist is smaller
			* than 2, it will add a event in the broker, so that broker can keep send this vm the rest of 
			* cloudlet in its batch queue
			**/	
		    case CLOUDLET_SUBMIT_RESUME: 
		    	resumeSubmitCloudlets(ev); 
		    	break;
		    case ESTIMATED_COMPLETION_TIME:
		    	setVmCompletionTime(ev);
		    	//submitCloudlets();
		    	break;

			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	private void setVmCompletionTime(SimEvent ev) {
		// TODO Auto-generated method stub
		if (ev.getData() instanceof Map) {
        	totalCompletionTime_vmMap =(Map) ev.getData();
        }
		
	}

	private void resumeSubmitCloudlets(SimEvent ev) {
		// TODO Auto-generated method stub
		submitCloudlets();
		
	}
	
	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
					+ " has been created in Datacenter #" + datacenterId + ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost().getId() + "\n");
		} else {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
					+ " failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();
		
		
		

		// all the requested VMs have been created
		//if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
		
		List<Vm> vmTempList = new ArrayList<Vm>();
		vmTempList.addAll(getVmsCreatedList());
		vmTempList.removeAll(vmDestroyedList);
		if(vmTempList.size() > 0){
		  Map<Integer, Double> totalCompletionTime_vmMap_temp = new HashMap<Integer, Double>();
		  totalCompletionTime_vmMap_temp = totalCompletionTime_vmMap;
			//Initial all vm completion time as 0.
			 for(Vm vm: vmTempList){
				    if(totalCompletionTime_vmMap_temp.containsKey(vm.getId())){
				    	totalCompletionTime_vmMap.put(vm.getId(), totalCompletionTime_vmMap_temp.get(vm.getId()));
				    }else{
				    	System.out.println("\ninitial vmcompletiontimemap test");
		        	    totalCompletionTime_vmMap.put(vm.getId(), 0.0);
				    }
		     }
			submitCloudlets();
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}
	
	
	//function for adding random delay with range
	private static int showDelayRandomInteger(int aStart, int aEnd, Random aRandom){
	    if (aStart > aEnd) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range, casting to long to avoid overflow problems
	    long range = (long)aEnd - (long)aStart + 1;
	    // compute a fraction of the range, 0 <= frac < range
	    long fraction = (long)(range * aRandom.nextDouble());
	    int randomNumber =  (int)(fraction + aStart);    
	    
	    return randomNumber;
	  }
	
	
	// this is the function for manipulation of cloudlets/tasks
	protected void submitCloudlets() { 
		int vmIndex = 0;
		int delay = 0; // delay for submission cloudlets
		Random randomDelay = new Random();// random delay generator object
		
		
	    List<Cloudlet> sortList = new ArrayList<Cloudlet>();
		
		//List<VTasks> sortList = new ArrayList<VTasks>();
		
		
		ArrayList<Cloudlet> tempList = new ArrayList<Cloudlet>();
		
		for (Cloudlet cloudlet : getCloudletList()) {
			tempList.add(cloudlet);
		}
		
		int totalCloudlets = tempList.size();
		//sorting the cloudlets from small to big in cloudlet length
		for(int i=0;i<totalCloudlets;i++) {
			Cloudlet smallestCloudlet = tempList.get(0);
			for(Cloudlet checkCloudlet:tempList) {
				if(smallestCloudlet.getCloudletLength()>checkCloudlet.getCloudletLength()) {
					smallestCloudlet = checkCloudlet;
				}
			}
			
			sortList.add(smallestCloudlet);
			tempList.remove(smallestCloudlet);
		}
		
		
		int count = 1;
		for(Cloudlet printCloudlet: sortList) {
			Log.printLine(count+" Cloudlet ID: "+printCloudlet.getCloudletId()+", Cloudlet Length: "+printCloudlet.getCloudletLength());
			count++;
		}
		
		
		//for (Cloudlet cloudlet : getCloudletList())
		
		for(Vm virm: getVmsCreatedList()) {
			//if(virm.getMips()>cloudlet.getCloudletLength()) {// this condition decides vms capacity for assiging task to vm
				//cloudlet.setVmId(virm.getId()); // May be this is the point where task assignment need to be done
				
				Log.printLine("Vm ID : " +virm.getId()+" Mips of Vm : "+virm.getMips());
			//}
	
		}
		
		
		
		for (Cloudlet cloudlet : sortList) {
			Vm vm;
			
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getVmId() == -1) {
				vm = getVmsCreatedList().get(vmIndex);
			} else { // submit to the specific vm
				
				
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId() + ": bount VM not available");
					continue;
				}
			}

			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
			
			

			
			
			//for(Vm virm: getVmsCreatedList()) {
				//if(virm.getMips()>cloudlet.getCloudletLength()) {// this condition decides vms capacity for assiging task to vm
					//cloudlet.setVmId(virm.getId()); // May be this is the point where task assignment need to be done
					
					//Log.printLine("Vm ID : " +virm.getId()+" Mips of Vm : "+virm.getMips());
				//}
		
			//}
			/*
			for(Vm checkVm: getVmsCreatedList()) {
				
				
				if(checkVm.getMips()>cloudlet.getCloudletLength()) {// this condition decides vms capacity for assiging task to vm
					cloudlet.setVmId(checkVm.getId()); // May be this is the point where task assignment need to be done
					Log.printLine("******************* Vm ID : " +checkVm.getId()+" Mips of Vm : "+checkVm.getMips()+" Cloudlet ID "+ cloudlet.getCloudletId()+" cloudlet size : "+cloudlet.getCloudletLength());
					schedule(getVmsToDatacentersMap().get(checkVm.getId()),delay ,CloudSimTags.CLOUDLET_SUBMIT, cloudlet); // controlling cloudlets delay
				
					Log.printLine("**** Cloudlet ID : "+cloudlet.getCloudletId()+" Cloudlet status ====  "+cloudlet.getCloudletFinishedSoFar());
					cloudletsSubmitted++;
					vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
					getCloudletSubmittedList().add(cloudlet);
					delay=delay+showDelayRandomInteger(1,10,randomDelay); // adding delay randomly between 1 to 10 seconds
					break;
				}
				else {
					continue;
				}
				
				
				//sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
				
				//schedule(getVmsToDatacentersMap().get(checkVm.getId()),delay ,CloudSimTags.CLOUDLET_SUBMIT, cloudlet); // controlling cloudlets delay
				//Log.printLine("**** Cloudlet ID : "+cloudlet.getCloudletId()+" Cloudlet status ====  "+cloudlet.getCloudletFinishedSoFar());
				//cloudletsSubmitted++;
				//vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
				//getCloudletSubmittedList().add(cloudlet);
				
				//System.out.println("Poisson number : "+getPoisson(showDelayRandomInteger(1,10,randomDelay)));
				//delay=delay+showDelayRandomInteger(1,10,randomDelay); // adding delay randomly between 1 to 10 seconds 
				
			
			}*/
			
			
			
			
			cloudlet.setVmId(vm.getId()); 
			
			
			Log.printLine("******************* Vm ID : " +vm.getId()+" Mips of Vm : "+vm.getMips()+" Cloudlet ID "+ cloudlet.getCloudletId()+" cloudlet size : "+cloudlet.getCloudletLength());
			
			schedule(getVmsToDatacentersMap().get(vm.getId()),delay ,CloudSimTags.CLOUDLET_SUBMIT, cloudlet); // controlling cloudlets delay
			Log.printLine("**** Cloudlet ID : "+cloudlet.getCloudletId()+" Cloudlet status ====  "+cloudlet.getCloudletFinishedSoFar());
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			
			//getVTasksSubmittedList().add(vtask);
			
			getCloudletSubmittedList().add(cloudlet);
			
			//System.out.println("Poisson number : "+getPoisson(showDelayRandomInteger(1,10,randomDelay)));
			delay=delay+showDelayRandomInteger(1,10,randomDelay); // adding delay randomly between 1 to 10 seconds 
			
			
			
			
			
		}

		// remove submitted cloudlets from waiting list
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
		
	}
	
	

}
