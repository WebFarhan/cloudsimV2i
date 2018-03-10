package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
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
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class testExecutionTime {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList1,cloudletList2,cloudletList3,taskDstrList;
	private static int SEED = 120;
	//private static List<VTasks> taskList;

	/** The vmlist. */
	private static List<Vm> vmlist1,vmlist2,vmlist3,vmlist4;
	private static ETCmatrix matrix;
	private static List<List<Vm>> VCs;
	private static int noTaskNotAllocated, taskMissDBS1, taskMissDBS2;
	public static int taskTypeNum = 3; //the number of task types
	public static int dataCenterNum = 3; //the number of data centers
	

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
	
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int START, int END, int idShift, long seed) throws NumberFormatException, IOException{
		
		   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();		/* Creates a container to store Cloudlets*/
		   double[] arrList = new double[5826];
			
	       File file = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival1.dat");
	        
	        BufferedReader br = new BufferedReader(new FileReader(file));
	        //Scanner scan = null;

	        try {
	            //scan = new Scanner(file);
	            int index = 0;
	            String line = null;
	            while ((line=br.readLine())!=null) {
	               
	                String[] lineArray = line.split(",");
	                // do something with lineArray, such as instantiate an object
	                
	                arrList[index]= Double.parseDouble(lineArray[2]);
	                //System.out.println(arrList[index]);
	                index++;
	            							} 
	        }catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } 
			
			/*Task (Cloudlets) parameters*/
			
			long length; 												/* MI of the Cloudlet */
			long fileSize = 540000;
			long outputSize = 300;
			int pesNumber = 1;
			double deadline = 0.0;
			double priority = 0.0;
			double xVal=0.0;
			int taskType = 0;
			//long seed1 = 500;

			//long timeMillis = System.currentTimeMillis();
	        //long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);
			
			UtilizationModel utilizationModel = new UtilizationModelFull();

			Cloudlet[] cloudlet = new Cloudlet[cloudlets];

			for(int i=0;i<cloudlets;i++){
				//long timeMillis = System.currentTimeMillis(); //replace with relative time to simulator
		        //long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);
				Random rObj = new Random();
				//rObj.setSeed(seed);
				deadline = showRandomDouble(0.4, 1.5);
				priority = Math.pow((1/Math.E),deadline);
				length = showRandomInteger(START, END,rObj);
				
				if(length <= 1500)
				{
					taskType = 1;
				}
				else if(length > 1500)
				{
					taskType = 2;
				}
				
				xVal = showRandomInteger(1,4,rObj);
				cloudlet[i] = new Cloudlet(taskType,idShift+i,length,deadline,priority,xVal,showRandomInteger(0,1,rObj),
						showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 25000,rObj), outputSize, utilizationModel, 
						utilizationModel, utilizationModel,arrList[i]);
				cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
				list.add(cloudlet[i]);
				seed--;
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

   private static double getConvolveProbability(double mu, double sigma, double deadLine) {
		
		try {
		NormalDistribution distr = new NormalDistribution(mu, sigma);
		return distr.cumulativeProbability(deadLine);
		}catch(NotStrictlyPositiveException exc) {
			return 0.0;
		}
	
	}
	
	
	
	//Load Balancer Function - Razin
	private static void loadBalancer(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3, ETCmatrix matrix, int trial  ) {
		
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
		
		
		if(trial == 0)
		{		
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
			} // end of for loop
		
		}
		else {
			
			System.out.println("!!!@@@##########  In ETC matrix !!!!&&&&&&&");
			
			for(Cloudlet cloudlet:cloudletList1)
			{
				
				if(cloudlet.getTaskType()==1) {
					
					if(cloudlet.getDeadline() > (matrix.getMu(1, 0)+ matrix.getSigma(1, 0))) {// task type 1
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
					}
					else if(cloudlet.getvHD()==1) {// task has its moving direction value.If it is 1 than it is moving right.
						if( cloudlet.getDeadline() > (matrix.getMu(1, 1)+matrix.getSigma(1, 1))){// task type 1, Base Station 1
							cloudlet.setUserId(broker2.getId());
							batchQueBS2.add(cloudlet);
						}
						else taskMissDBS1++;
					}
					else if(cloudlet.getvHD()==0) {// task has its moving direction value.If it is 0 than it is moving left.
						if(cloudlet.getDeadline() > (matrix.getMu(1,2)+matrix.getSigma(1,2))){
							cloudlet.setUserId(broker3.getId());
							batchQueBS3.add(cloudlet);
						}
						else taskMissDBS2++;
					}
					else {
						noTaskNotAllocated++;
					}
					
		
				}// End of task type 1
				else if(cloudlet.getTaskType()==2) {// task type 2
					
					if(cloudlet.getDeadline()> (matrix.getMu(2, 0) + matrix.getSigma(2, 0))) {// task type 2, Base Station 0
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
					}
					else if(cloudlet.getvHD()==1) {// task has its moving direction value.If it is 1 than it is moving right.
						if( cloudlet.getDeadline() > (matrix.getMu(2, 1)+matrix.getSigma(2, 1))){// task type 1, Base Station 1
							cloudlet.setUserId(broker2.getId());
							batchQueBS2.add(cloudlet);
						}
						else taskMissDBS1++;
					}
					else if(cloudlet.getvHD()==0) {// task has its moving direction value.If it is 0 than it is moving left.
						if(cloudlet.getDeadline() > (matrix.getMu(2,2)+matrix.getSigma(2,2))){
							cloudlet.setUserId(broker3.getId());
							batchQueBS3.add(cloudlet);
						}
						else taskMissDBS2++;
					}
					else {
						noTaskNotAllocated++;
					}
					
				}// End of task type 2
			
			
			}
		}
		
		broker1.submitCloudletList(batchQueBS1); // submitting cloudlets to a Base Station 0 where tasks with deadline less than or equal 2 sec.
		broker2.submitCloudletList(batchQueBS2); // submitting cloudlets to a Base Station 01where tasks with deadline greater than 2 sec.
		broker3.submitCloudletList(batchQueBS3);
		
	}
	
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		// First step: Initialize the CloudSim package. It should be called
		// before creating any entities.
		int num_user = 1;   // number of grid users
		Calendar calendar = Calendar.getInstance();
		boolean trace_flag = false;  // mean trace events

		FileWriter fw = new FileWriter("executionTime.txt",true);
		PrintWriter printWriter = new PrintWriter(fw);
		
		// Initialize the CloudSim library
		CloudSim.init(num_user, calendar, trace_flag);
		
		// Second step: Create 3 Datacenters/Base Station 
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
		broker1.setSchedulerPolicy("FCFS");//setting scheduler policy_Razin
		
		DatacenterBroker broker2 = createBroker("broker2");//create broker 2
		int vmMips2 = 2000;
		vmlist2 = createVM_N(broker2.getId(), 5,vmMips2, 1001);
		broker2.submitVmList(vmlist2);
		broker2.setSchedulerPolicy("FCFS");//setting scheduler policy_Razin
		
		DatacenterBroker broker3 = createBroker("broker3");//create broker 3
		int vmMips3 = 3000;
		vmlist3 = createVM_N(broker3.getId(), 5,vmMips3, 2001);
		broker3.submitVmList(vmlist3);
		broker3.setSchedulerPolicy("FCFS");//setting scheduler policy_Razin
		//End of broker creation - Razin
		
		cloudletList1 = createCloudlet(broker1.getId(),10,100,2000,1,400);// this is the arrival buffer
		
		cloudletList2 = createCloudlet(broker1.getId(),10,100,2000,100,400);// this is the arrival buffer
		
		
		
		for(Cloudlet cloudlet:cloudletList1) {
			
			System.out.println(" Cloudlet Status :::@@@@::: " + cloudlet.getCloudletStatus());
			
		}
		
		//double result= getConvolveProbability(1.6673,0.7777,0.7689);
		
		
		//System.out.println(" Result      +++++++ " + result);
		
		broker1.submitCloudletList(cloudletList1);
		
		//broker1.submitVmList(vmlist1);
		broker1.submitCloudletList(cloudletList2);
		
		CloudSim.startSimulation();
		
		
		
		
		//CloudSim.pauseSimulation();
		//if(CloudSim.pauseSimulation()== true)
			//System.out.println("simulation is paused");
		//else System.out.println("Not pause");
		
		// Final step: Print results when simulation is over
		
		

			
     	//List<VTasks> newList1 = broker1.getVTasksReceivedList();
					
		//newList.addAll(broker2.getCloudletReceivedList());
		//newList.addAll(broker3.getCloudletReceivedList());

		CloudSim.stopSimulation();
		
		List<Cloudlet> newList = broker1.getCloudletReceivedList();
		
       for(Cloudlet cloudlet:newList) {
			
			System.out.println(cloudlet.getCloudletId() + " Cloudlet waiting time :::@@@@::: " + cloudlet.getWaitingTime());
			
		}
		
		
		
		printCloudletList(newList);

		//printWriter.println("         Execution time  "+"     "+"    Tasks MI");
		
		DecimalFormat dft = new DecimalFormat("###.##");
		for(Cloudlet cloudlet:newList) {
			
			//System.out.println("  file size  "+cloudlet.getCloudletFileSize());
			
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {	

				//printWriter.println(dft.format(cloudlet.getActualCPUTime()) + " " + cloudlet.getCloudletTotalLength());
		
			}
			else {
				//printWriter.println("Not successful");	
			
			}
		}
		Log.printLine("V2I task processing finished!");
		printWriter.close();
		

}
	
	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 * @throws IOException 
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;
		
		//FileWriter fileWriter = new FileWriter("ResultV2I.txt",true);
		//PrintWriter printWriter = new PrintWriter(fileWriter);	
		
		
		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent+indent+ "VM ID" + indent + indent+"  "+ "Time"+indent+indent +"Task Length"+ indent+indent +"Task Type"+indent+indent+ "Start Time" + indent + "Finish Time"+indent+indent+"Deadline"+indent+indent+"Completion Time");
        
				DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);
			
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");
				
				Log.printLine( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent + cloudlet.getCloudletLength()+ indent + indent +indent+indent+ cloudlet.getTaskType()+indent+indent+indent+indent+dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+cloudlet.getDeadline()+indent+indent+indent+(cloudlet.getFinishTime() - cloudlet.getSubmissionTime()));
				
				
			}
		}

		//printWriter.close();
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
		//for a five-core machine, a list of 4 PEs is required:
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
			datacenter = new Datacenter(name,x ,54000000, range,characteristics, new VmAllocationPolicySimple(hostList), storageList, 100);
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
	
	
}
