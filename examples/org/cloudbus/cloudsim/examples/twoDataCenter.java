/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */


package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.print.attribute.standard.PrinterLocation;

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
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import com.sun.java_cup.internal.runtime.Scanner;
import com.sun.java_cup.internal.runtime.Symbol;


/**
 * An example showing how to create
 * scalable simulations.
 */
public class twoDataCenter {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList1,cloudletList2,cloudletList3;
	private static int SEED = 120;
	//private static List<VTasks> taskList;

	/** The vmlist. */
	private static List<Vm> vmlist1,vmlist2,vmlist3,vmlist4;
	private static List<List<Vm>> VCs;
	private static int noTaskNotAllocated, taskMissDBS1, taskMissDBS2;
	

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
        int hmips = 100;
        //create VMs
        Vm[] vm = new Vm[vms];
        
        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
            //hmips= hmips+500;
        }
        
        return list;
    }
	
    // creating the tasks(cloudlets) for base stations
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int START, int END, int idShift, long seed){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//tasks(Cloudlets) parameters
		long length; // mi of cloudlet
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		double deadline = 0.0;
		double priority = 0.0;
		double xVal=0.0;
		String taskType= null;
		//long seed1 = 500;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			Random rObj = new Random();
			rObj.setSeed(seed);
			deadline = showRandomDouble(0.4, 1.5);
			priority = Math.pow((1/Math.E),deadline);
			length = 1000+showRandomInteger(START, END,rObj);
			
			if(length > 1000 && length <1500)
			{
				taskType = "Type1";
			}
			else if(length > 1500 && length <2000)
			{
				taskType = "Type2";
			}
			
			xVal = showRandomInteger(1,4,rObj);
			cloudlet[i] = new Cloudlet(taskType,idShift+i,length,deadline,priority,xVal,showRandomInteger(0,1,rObj),showRandomInteger(120,120,rObj),pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
			seed--;
		}

		return list;
	}
	 
	//function for randomly generate task for different task arriving situations. This can be used for creating over subscribe situation.
	private static int generateTasksRandomly(int aStart, int aEnd, Random aRandom){
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
	
	//calculate deadline for a task with respect to Base Station range.currently not using
	public static double Deadline(Cloudlet a, Datacenter d) {
		
		double deadline = d.getRange()/(a.getvSpeed()*1609.34);
		
		System.out.println("------Deadline of task : "+a.getCloudletId()+ " is : "+(deadline*3600));
		return (deadline*3600);
	}
	
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	private static double showRandomDouble(double aStart, double aEnd){
	    if (aStart > aEnd) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }

	    Random r = new Random();
	    double randomValue = aStart + (aEnd - aStart) * r.nextDouble();
	    
	    
	    return round(randomValue, 2);//randomValue;
	  }

	private static double zScoreCalculation(double mu, double sigma, double value) {
		
		double result = 0;
		
		result = (value - mu)/sigma;
		
		return round(result,2);
	}
	
	
	private static void loadBalancer(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3) {
		
		// three batch queue for three Base Station
		ArrayList<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		ArrayList<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		ArrayList<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
					
		double slacktime1 = 0.25;
		double slacktime2 = 0.15;
		double slacktime3 = 0.27;
		noTaskNotAllocated = 0;
					
		taskMissDBS1 = 0;
		taskMissDBS2 = 0;
		
		for(Cloudlet cloudlet:cloudletList1)
		{
			//Long l2=Long.valueOf(vmMips1);
			double executionTimeVM1 = cloudlet.getCloudletLength() / (double)vmMips1;
			double executionTimeVM2 = cloudlet.getCloudletLength() / (double)vmMips2;
			double executionTimeVM3 = cloudlet.getCloudletLength() / (double)vmMips3;
			
			if(cloudlet.getDeadline()> (executionTimeVM1+slacktime1)) {
				cloudlet.setUserId(broker1.getId());
				batchQueBS1.add(cloudlet);
			}
			else if(cloudlet.getvHD()==1) {// task has its moving direction value.If it is 1 than it is moving right.
				if( cloudlet.getDeadline() > (executionTimeVM2+slacktime2)){
					cloudlet.setUserId(broker2.getId());
					batchQueBS2.add(cloudlet);
				}
				else taskMissDBS1++;
				
			}
			else if(cloudlet.getvHD()==0) {// task has its moving direction value.If it is 0 than it is moving left.
				if(cloudlet.getDeadline() > (executionTimeVM3+slacktime3)){
					cloudlet.setUserId(broker3.getId());
					batchQueBS3.add(cloudlet);
				}
				else taskMissDBS2++;
			}
			else {
				noTaskNotAllocated++;
			}
		}
		
		broker1.submitCloudletList(batchQueBS1); // submitting cloudlets to a Base Station 0 where tasks with deadline less than or equal 2 sec.
		
		broker2.submitCloudletList(batchQueBS2); // submitting cloudlets to a Base Station 01where tasks with deadline greater than 2 sec.
		
		broker3.submitCloudletList(batchQueBS3);
	
		
	}
	
	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Log.printLine("Starting Simulation for V2I task processing...");
		int number = 0;
		int arrivingTasks = 0;
		boolean lbOnOff=false;
		String SchPolicy=null; 
		
		FileWriter fw = new FileWriter("ResultV2I.txt",true);
		PrintWriter printWriter = new PrintWriter(fw);
		printWriter.println();
		printWriter.println("Starting Simulation for V2I task processing...");
		
		try
		{
		File file = new File("input.txt"); // input file
	
		BufferedReader br = new BufferedReader(new FileReader(file));
			try {
				String NO_Trail = br.readLine();
				number = Integer.parseInt(NO_Trail);
				String secondStr = br.readLine();
				arrivingTasks = Integer.parseInt(secondStr);
				String LB = br.readLine();
				lbOnOff = Boolean.parseBoolean(LB);
				SchPolicy = br.readLine();
				
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 
			}
			catch (FileNotFoundException fnfe)
			{
				System.out.println("File data.txt was not found!");
			} 
		printWriter.println("Number of Total trial : " + number);
		printWriter.println("Load Balancer Status :"+lbOnOff);
		printWriter.println("Scheduler Policy :"+SchPolicy);
		printWriter.println();
		
		for(int i = 0;i <number;i++) {
		try {
			//FileWriter fw = new FileWriter("ResultV2I.txt",true);
			//PrintWriter printWriter = new PrintWriter(fw);
			//printWriter.println("Number of Total trial : " + number);
			printWriter.println("Trial No : "+ i);
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");// added by Razin

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create 3 Datacenters
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("BaseStation_0",1,0,60);//Base Station x coordinate 0 and range is 60 meter
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("BaseStation_1",1,5,65);// Base Station x coordinate 5 and range is 65 meter
			@SuppressWarnings("unused")
			Datacenter datacenter2 = createDatacenter("BaseStation_2",1,-2.5,50);// Base Station x coordinate -2.5 and range is 50 meter
			
			//Third step: Create Broker
			DatacenterBroker broker1 = createBroker("broker1");// create broker 1
			int vmMips1 = 1500;
			vmlist1 = createVM_N(broker1.getId(), 5,vmMips1, 1);
			broker1.submitVmList(vmlist1);
			broker1.setSchedulerPolicy(SchPolicy);//setting scheduler policy_Razin
			
			DatacenterBroker broker2 = createBroker("broker2");//create broker 2
			int vmMips2 = 2000;
			vmlist2 = createVM_N(broker2.getId(), 5,vmMips2, 1001);
			broker2.submitVmList(vmlist2);
			broker2.setSchedulerPolicy(SchPolicy);//setting scheduler policy_Razin
			
			DatacenterBroker broker3 = createBroker("broker3");//create broker 3
			int vmMips3 = 3000;
			vmlist3 = createVM_N(broker3.getId(), 5,vmMips3, 2001);
			broker3.submitVmList(vmlist3);
			broker3.setSchedulerPolicy(SchPolicy);//setting scheduler policy_Razin
			
			int numTasksAdmitted = arrivingTasks;// set the number of tasks coming to Base Station
			cloudletList1 = createCloudlet(broker1.getId(),numTasksAdmitted,100,1000,1,500);// this is the arrival buffer 
			
			
			for(Cloudlet cloudlet:cloudletList1) {				
				System.out.println("****** cloudlet ID "+cloudlet.getCloudletId() +" Cloudlet Length : " + cloudlet.getCloudletLength() + " Start time " +cloudlet.getExecStartTime());
			}
			
			if (lbOnOff == true) {
				loadBalancer(cloudletList1, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3);
			}
			else {
				broker1.submitCloudletList(cloudletList1);
				
			}
			
			//loadBalancer(cloudletList1, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3);
			
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
							CloudSim.resumeSimulation();
						}
			};

			new Thread(monitor).start();
			Thread.sleep(1000);
			*/

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker1.getCloudletReceivedList();
	
			//List<VTasks> newList1 = broker1.getVTasksReceivedList();
			
			newList.addAll(broker2.getCloudletReceivedList());
			newList.addAll(broker3.getCloudletReceivedList());

			CloudSim.stopSimulation();

			
			printCloudletList(newList,printWriter);
			
			double deadLineMissRate = noTaskNotAllocated / (double)numTasksAdmitted;
			System.out.println("No of task not allocated : "+ noTaskNotAllocated +" Allocation miss rate : "+ deadLineMissRate);
			printWriter.println("No of task not allocated : "+ noTaskNotAllocated +" Allocation miss rate : "+ deadLineMissRate);
			
			System.out.println("No of task missed deadline in Base Station 1 : " + taskMissDBS1);
			printWriter.println("No of task missed deadline in Base Station 1 : " + taskMissDBS1);
			
			System.out.println("No of task missed deadline in Base Station 2 : " + taskMissDBS2);
			printWriter.println("No of task missed deadline in Base Station 2 : " + taskMissDBS2);
			
			Log.printLine("V2I task processing finished!");

			printWriter.println();
			printWriter.println("End of trail : "+i);
			//System.out.println("Z score :"+ zScore);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			printWriter.println("The simulation has been terminated due to an unexpected error");
			
		}
		
	}//end of for loop
		printWriter.println("V2I task processing finished!");
		printWriter.println("********************************************************************************************************");
		printWriter.close();
		
	}

	private static Datacenter createDatacenter(String name, int hostNumber, double x,double range){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 40000; // this is the mips for each core of host of datacenter

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(4, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		
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
    				new VmSchedulerSpaceShared(peList1)// SpaceScheduler distribute VMs to different data centers
    			)
    		); // This is our first machine

			hostId++;
		}


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
		//double range = 5000;
		try {
			datacenter = new Datacenter(name,x , range,characteristics, new VmAllocationPolicySimple(hostList), storageList, 100);
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
	 * @throws IOException 
	 */
	private static void printCloudletList(List<Cloudlet> list, PrintWriter pw) throws IOException {
		int size = list.size();
		Cloudlet cloudlet;
		
		//FileWriter fileWriter = new FileWriter("ResultV2I.txt",true);
		//PrintWriter printWriter = new PrintWriter(fileWriter);	
		
		
		String indent = "    ";
		Log.printLine();
		pw.println();//file write
		Log.printLine("========== OUTPUT ==========");
		pw.println("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent+indent+ "VM ID" + indent + indent+"  "+ "Time"+indent+indent +"Task Length"+ indent+indent + "Start Time" + indent + "Finish Time"+indent+indent+"Deadline"+indent+indent+"Priority");
        
		pw.println("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent+indent+ "VM ID" + indent + indent+"  "+ "Time"+indent+indent +"Task Length"+ indent+indent + "Start Time" + indent + "Finish Time"+indent+indent+"Deadline"+indent+indent+"Priority");
		
		
		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);
			
			pw.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");
				pw.print("SUCCESS");
				
				Log.printLine( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + cloudlet.getCloudletLength()+ indent + indent +indent +dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+cloudlet.getDeadline()+indent+indent+cloudlet.getPriority());
				
				pw.println( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + cloudlet.getCloudletLength()+ indent + indent +indent +dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+cloudlet.getDeadline()+indent+indent+cloudlet.getPriority());
			}
		}

		//printWriter.close();
	}
	
	
	 //Inner-Class GLOBAL BROKER...
    public static class GlobalBroker extends SimEntity {
        
        private static final int CREATE_BROKER = 0;
        private List<Vm> vmList;
        private List<Cloudlet> cloudletList;
        
        //private List<VTasks> vtaskList;
        
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
                    //setVmList(createVM(getBroker().getId(), 5, 100)); //creating 5 vms
                    //setCloudletList(createCloudlet(getBroker().getId(), 10, 100)); // creating 10 cloudlets
                   
                    broker.submitVmList(getVmList());
                    broker.submitCloudletList(getCloudletList());

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
        
        /*
        //for vtasklist
        public List<VTasks> getVTaskList() {
            return vtaskList;
        }
        */
        
        protected void setCloudletList(List<Cloudlet> cloudletList) {
            this.cloudletList = cloudletList;
        }
        
        /*
        protected void setVTaskList(List<VTasks> vtaskList) {
            this.vtaskList = vtaskList;
        }
        */
        
        public DatacenterBroker getBroker() {
            return broker;
        }
        
        protected void setBroker(DatacenterBroker broker) {
            this.broker = broker;
        }
    }
}

