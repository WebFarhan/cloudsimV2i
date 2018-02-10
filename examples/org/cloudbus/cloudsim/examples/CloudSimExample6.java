/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */


package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


/**
 * An example showing how to create
 * scalable simulations.
 */
public class CloudSimExample6 {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList1,cloudletList2;

	/** The vmlist. */
	private static List<Vm> vmlist1,vmlist2;
	
	
	//All the instance share cloudletNewArrivalQueue and cloudletBatchqueue, both of them are synchronized list
	private static List<Cloudlet> cloudletNewArrivalQueue = Collections.synchronizedList(new ArrayList<Cloudlet>());
	private static List<Cloudlet> cloudletBatchQueue = Collections.synchronizedList(new ArrayList<Cloudlet>());
	
	public static Properties prop = new Properties();
	

	private static List<Vm> createVM_N(int userId, int vms, int mips, int idShift) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<Vm> list = new LinkedList<Vm>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        //int mips = 2000;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        Vm[] vm = new Vm[vms];
        
        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        
        return list;
    }
	
	
	private static List<Vm> createVM(int userId, int vms) {

		//Creates a container to store VMs(Base Stations). This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		int mips = 1000;
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
			//for creating a VM with a space shared scheduling policy for cloudlets:
			//vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority, vmm, new CloudletSchedulerSpaceShared());

			list.add(vm[i]);
		}

		return list;
	}

	// creating 2 different VM(Base Stations) 
	
	private static List<Vm> create2VM(int userId) {

		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM_1 Parameters ---- weak one
		long size_1 = 10000; //image size (MB)
		int ram_1 = 512; //vm memory (MB)
		int mips_1 = 3000;
		long bw_1 = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name
		
		//VM_2 Parameters ---- strong one
		long size_2 = 8000; //image size (MB)
		int ram_2 = 1024; //vm memory (MB)
		int mips_2 = 5000;
		long bw_2 = 2000;
		

		//create VMs(Base Stations)
		Vm[] vm = new Vm[2];

		vm[0] = new Vm(0, userId, mips_1, pesNumber, ram_1, bw_1, size_1, vmm, new CloudletSchedulerSpaceShared());
			list.add(vm[0]);
			
		vm[1] =  new Vm(1, userId, mips_2, pesNumber, ram_2, bw_2, size_2, vmm, new CloudletSchedulerSpaceShared());
			list.add(vm[1]);
			//for creating a VM with a space shared scheduling policy for cloudlets:
			
		return list;
	}
	
    // creating the tasks(cloudlets) for base stations
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int START, int END, int idShift){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//tasks(Cloudlets) parameters
		long length = 1000; // mips of cloudlet
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			Random rObj = new Random();
			
			cloudlet[i] = new Cloudlet(idShift+i, (length+showRandomInteger(START, END,rObj)), pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	private static int showRandomInteger(int aStart, int aEnd, Random aRandom){
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
	
	//customized cloudlet creation for testing
	private static List<Cloudlet> create3Cloudlet(int userId){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
		
		//task 1(Cloudlets) parameters
		long length1 = 1000;
		long fileSize1 = 300;
		long outputSize1 = 300;
		int pesNumber1 = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();
		
		Cloudlet[] cloudlet = new Cloudlet[3];
		
		cloudlet[0] = new Cloudlet(0, length1, pesNumber1, fileSize1, outputSize1, utilizationModel, utilizationModel, utilizationModel);
		cloudlet[0].setUserId(userId);
		list.add(cloudlet[0]);
		
		//task 2(Cloudlets) parameters
		long length2 = 1000;
		long fileSize2 = 300;
		long outputSize2 = 300;
		int pesNumber2 = 1;

		
		cloudlet[1] = new Cloudlet(1, length2, pesNumber2, fileSize2, outputSize2, utilizationModel, utilizationModel, utilizationModel);
		cloudlet[1].setUserId(userId);
		list.add(cloudlet[1]);
		
		
		//task 3(Cloudlets) parameters
		long length3 = 1000;
		long fileSize3 = 300;
		long outputSize3 = 300;
		int pesNumber3 = 1;
		
		cloudlet[2] = new Cloudlet(2, length3, pesNumber3, fileSize3, outputSize3, utilizationModel, utilizationModel, utilizationModel);
		cloudlet[2].setUserId(userId);
		list.add(cloudlet[2]);
		
		return list;
	}
	
	

	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		Log.printLine("Starting Simulation for V2I task processing...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			//@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("BaseStation_0",1);
			//@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("BaseStation_1",1);

			//Third step: Create Broker
			DatacenterBroker broker1 = createBroker("broker1");
			broker1.submitVmList(createVM_N(broker1.getId(), 5,1000, 1));
			broker1.submitCloudletList(createCloudlet(broker1.getId(), 10,100,200,1));
			
			
			DatacenterBroker broker2 = createBroker("broker2");
			broker2.submitVmList(createVM_N(broker2.getId(), 5, 2000, 1001));
			broker2.submitCloudletList(createCloudlet(broker2.getId(), 10,100,200,1001));
			
			//int brokerId1 = broker1.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			//int numOfVM = 1;
			//vmlist1 = createVM(brokerId,5); //creating 20 vms
			//vmlist2 = createVM(brokerId,1);
			//vmlist2 = createVM(brokerId,1);
			//vmlist = create2VM(brokerId);// customized function to create 2 VMs(Base Stations) weak & strong 
			
			
			//cloudletList = create3Cloudlet(brokerId);
			
			//cloudletList1 = createCloudlet(brokerId,40,100,200); // creating 40 cloudlets
			//cloudletList2 = createCloudlet(brokerId,20); // creating 20 cloudlets

			//broker.submitVmList(vmlist1);
			//broker.submitCloudletList(cloudletList1);
			
			// A thread that will create a new broker at 200 clock time
		/*	Runnable monitor = new Runnable() {
					@Override
					public void run() {
						CloudSim.pauseSimulation(200);
						while (true) {
							if (CloudSim.isPaused()) {
								break;
							}
							try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

						Log.printLine("\n\n\n" + CloudSim.clock() + ": The simulation is paused for 3 sec \n\n");

						try {
							Thread.sleep(3000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							DatacenterBroker broker1 = createBroker();
							int brokerId = broker1.getId();

							//Create VMs and Cloudlets and send them to broker
							vmlist2 = createVM(brokerId, 5); //creating 5 vms
							cloudletList2 = createCloudlet(brokerId, 10,100,200); // creating 10 cloudlets

							broker1.submitVmList(vmlist2);
							broker1.submitCloudletList(cloudletList2);

							CloudSim.resumeSimulation();
						}
			};

			new Thread(monitor).start();
			Thread.sleep(1000);
			*/

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();
			
			

			//broker.submitVmList(vmlist2);
			//broker.submitCloudletList(cloudletList2);
			

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker1.getCloudletReceivedList();
			 newList.addAll(broker2.getCloudletReceivedList());

			CloudSim.stopSimulation();

			printCloudletList(newList);
			
			Log.printLine("V2I task processing finished!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static Datacenter createDatacenter(String name, int hostNumber){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		//int mips = 20000; // this is the mips for each core of host of datacenter

		// 3. Create PEs and add these into the list.
		//for a 5 core machine, a list of 5 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(20000))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(10000)));
		peList1.add(new Pe(2, new PeProvisionerSimple(30000)));
		peList1.add(new Pe(3, new PeProvisionerSimple(40000)));
		peList1.add(new Pe(4, new PeProvisionerSimple(30000))); // need to store Pe id and MIPS Rating
		
		//peList1.add(new Pe(5, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(6, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(7, new PeProvisionerSimple(mips)));
		
		
		//Another list, for a dual-core machine
		//List<Pe> peList2 = new ArrayList<Pe>();

		//peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		//peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int hostId=0;
		int ram = 8192; //host memory (MB) 8 GB given by Razin
		long storage = 1000000; //host storage
		int bw = 20000;

		for (int i = 0; i < hostNumber; i++) {
		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerSpaceShared(peList1)
    			)
    		); // This is our first machine

			hostId++;
		}
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			peList2,
    	//			new VmSchedulerTimeShared(peList2)
    	//		)
    	//	); // Second machine


		//To create a host with a space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerSpaceShared(peList1)
    	//		)
    	//	);

		//To create a host with a oportunistic space-shared allocation policy for PEs to VMs:
		//hostList.add(
    	//		new Host(
    	//			hostId,
    	//			new CpuProvisionerSimple(peList1),
    	//			new RamProvisionerSimple(ram),
    	//			new BwProvisionerSimple(bw),
    	//			storage,
    	//			new VmSchedulerOportunisticSpaceShared(peList1)
    	//		)
    	//	);


		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		String arch = "x86";      // system architecture
		String os = "Linux";          // operating system
		String vmm = "Xen";
		double time_zone = 10.0;         // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.1;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	//Need to develop own broker policies, to submit vms and cloudlets according
	//to the specific rules of the simulated scenario
	private static DatacenterBroker createBroker(String name){

		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent+indent+ "VM ID" + indent + indent+"  "+ "Time"+indent+indent +"Task Length"+ indent+indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");

				Log.printLine( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + cloudlet.getCloudletLength()+ indent + indent +indent +dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime()));
			}
		}

	}
	
	
	 //Inner-Class GLOBAL BROKER...
    public static class GlobalBroker extends SimEntity {
        
        private static final int CREATE_BROKER = 0;
        private List<Vm> vmList;
        private List<Cloudlet> cloudletList;
        private DatacenterBroker broker;
        
        public GlobalBroker(String name) {
            super(name);
        }
        
        @Override
        public void processEvent(SimEvent ev) {
            switch (ev.getTag()) {
                case CREATE_BROKER:
                    setBroker(createBroker(super.getName() + "_"));

                    //Create VMs and Cloudlets and send them to broker
//                    setVmList(createVM(getBroker().getId(), 5, 100)); //creating 5 vms
//                    setCloudletList(createCloudlet(getBroker().getId(), 10, 100)); // creating 10 cloudlets

                    
                    broker.submitVmList(getVmList());
                    broker.submitCloudletList(getCloudletList());

//                    CloudSim.resumeSimulation();

                    break;
                
                default:
                    Log.printLine(getName() + ": unknown event type");
                    break;
            }
        }
        
        @Override
        public void startEntity() {
            Log.printLine(CloudSim.clock() + super.getName() + " is starting...");
            schedule(getId(), 200, CREATE_BROKER);
            
        }
        
        @Override
        public void shutdownEntity() {
            System.out.println("Global Broker is shutting down...");
        }
        
        public List<Vm> getVmList() {
            return vmList;
        }
        
        protected void setVmList(List<Vm> vmList) {
            this.vmList = vmList;
        }
        
        public List<Cloudlet> getCloudletList() {
            return cloudletList;
        }
        
        protected void setCloudletList(List<Cloudlet> cloudletList) {
            this.cloudletList = cloudletList;
        }
        
        public DatacenterBroker getBroker() {
            return broker;
        }
        
        protected void setBroker(DatacenterBroker broker) {
            this.broker = broker;
        }
    }
}

