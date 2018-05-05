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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;



public class smartOilField {

	
	private static List<Cloudlet> cloudletList3,cloudletList4,cloudletList5,cloudletList6,cloudletList7, cloudletList8, cloudletList9, taskDstrList;
	private static List<Cloudlet> executedList;
	
	private static ArrayList<Integer> allCounters = new ArrayList<>();
	private static ArrayList<Integer> allTaskNotAllocated = new ArrayList<>();
	private static List<Vm> vmlist1, vmlist2, vmlist3;
	private static ETCmatrix matrix;
	private static ETTmatrix ettMatrix;
	private static HashMap<String, NormalDistribution> distributions = new HashMap<>();
	private static HashMap<String, NormalDistribution> trTimes = new HashMap<>();
	private static ListMultimap<String, Double> executionTimes = ArrayListMultimap.create();
	private static ListMultimap<String, Double> transferTimes = ArrayListMultimap.create();
	private static int noTaskNotAllocated;
	public static int taskTypeNum = 3; 										 /*the number of task types*/
	public static int dataCenterNum = 2;										 /*the number of data centers*/
	public static Properties prop = new Properties();
	private static int numTaskAllocatedBS1, numTaskAllocatedBS2, numTaskAllocatedBS3;
	private static int initialWorkload = 1000;
	
	private static int numTasksAdmitted;
	
	
	/**Creates a container to store VMs. 
	 * This list is passed to the broker later
	 * @param userId
	 * @param vms
	 * @param mips
	 * @param idShift
	 * @return
	 */
	
	private static List<Vm> createVM_N(int userId, int vms, int mips, int idShift) {
        	
        LinkedList<Vm> list = new LinkedList<Vm>();

        /*VM Parameters*/
        
        long size = 10000; 							/*Image size (MB)*/
        int ram = 512; 								/*VM's memory (MB)*/
        long bw = 1000;
        int pesNumber = 1; 							/*Number of CPUs*/
        String vmm = "Xen"; 							/*VMM Name*/
    
        /*Create Virtual Machines*/
        
        Vm[] vm = new Vm[vms];
        
        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }

	
	private static int gaussianMiGenerator(double sd,double mean, Random r,double mincap,double maxcap) {
		
		double Result = mean+r.nextGaussian()*sd;
		if(Result < mincap) {
			Result = 2*mincap-Result;
		}
		else if(Result > maxcap) {
		Result=Math.abs(2*maxcap- Result);
			
		}
		return (int)Result;
	}
	
    /** Create tasks (Cloudlets) for the base stations
     * @param userId
     * @param cloudlets
     * @param START
     * @param END
     * @param idShift
     * @param seed
     * @return
     * @throws IOException 
     * @throws NumberFormatException 
     */
	
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int START, int END, int idShift, long seed, int filee) throws NumberFormatException, IOException{
		
	   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();		/* Creates a container to store Cloudlets*/
	   double[] arrList = new double[26000];
		
       File file = new File("/home/c00303945/Downloads/cloudsim-3.0.3/denseArrival"+filee+".dat");
        
       BufferedReader br = new BufferedReader(new FileReader(file));
       
       try {
           int index = 0;
           String line = null;
           while ((line=br.readLine())!=null) {
              
               String[] lineArray = line.split(",");
               arrList[index]= Double.parseDouble(lineArray[2]);
               index++;
           							} 
       }catch (FileNotFoundException e) {
           e.printStackTrace();
       } 
		
		/*Task (Cloudlets) parameters*/
		
		long length; 												/* MI of the Cloudlet */
		long fileSize = 4000000;
		long outputSize = 300;
		int pesNumber = 1;
		double deadline = 0.0;
		double priority = 0.0;
		double xVal=0.0;
		int taskType = 0;
		
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		Random rObj = new Random();
		rObj.setSeed(seed);
		
		for(int i=0;i<cloudlets;i++){
			
			priority = Math.pow((1/Math.E),deadline);
			
			length = gaussianMiGenerator(1500, 8000,rObj,START,END);
			

			if(length >= 3500 && length < 6500)
			{
				taskType = 1;
			}
			else if(length >= 6500 && length < 9500)
			{
				taskType = 2;
				fileSize = fileSize*taskType;
			}
			else if(length >=9500 && length < 12500) {
				
				taskType = 3;
				fileSize = fileSize*taskType;
				
			}
			
			xVal = showRandomInteger(1,4,rObj);
			cloudlet[i] = new Cloudlet(taskType,idShift+i,length,deadline,priority,xVal,showRandomInteger(0,1,rObj),
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(1000000, 2000000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,arrList[i]);
			cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
			list.add(cloudlet[i]);
		}

		return list;
	}
	
	/** Create tasks (Cloudlets) for the base stations
     * @param userId
     * @param cloudlets
     * @param START
     * @param END
     * @param idShift
     * @param seed
     * @return
     * @throws IOException 
     * @throws NumberFormatException 
     */
	
	private static List<Cloudlet> createInitialWorkLoad(int userId, int cloudlets, int START, int END, int idShift, long seed) throws NumberFormatException, IOException{
		
	   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();	/* Creates a container to store Cloudlets*/
	   double[] arrList = new double[5826];
		
       File file = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival1.dat");
        
       BufferedReader br = new BufferedReader(new FileReader(file));
       
       try {
           int index = 0;
           String line = null;
           while ((line=br.readLine())!=null) {
              
               String[] lineArray = line.split(",");
               arrList[index]= Double.parseDouble(lineArray[2]);
               index++;
           							} 
       }catch (FileNotFoundException e) {
           e.printStackTrace();
       } /* Creates a container to store Cloudlets*/
	   	/*Task (Cloudlets) parameters*/
		
		long length; 												/* MI of the Cloudlet */
		long fileSize = 1400000;
		long outputSize = 300;
		int pesNumber = 1;
		double deadline = 0.0;
		double priority = 0.0;
		double xVal=0.0;
		int taskType = 0;
		
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		Random rObj = new Random();
		rObj.setSeed(seed);
		
		for(int i=0;i<cloudlets;i++){
			
			priority = Math.pow((1/Math.E),deadline);
			length = gaussianMiGenerator(400, 10000,rObj,START,END);
			
			if(length <= 10000)
			{
				taskType = 1;
			}
			else if(length > 10000)
			{
				taskType = 2;
			}
			
			xVal = showRandomInteger(1,4,rObj);
			cloudlet[i] = new Cloudlet(taskType,idShift+i,length,deadline,priority,xVal,showRandomInteger(0,1,rObj),
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 20000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,arrList[cloudlets]);
			cloudlet[i].setUserId(userId);		
			/* Setting the owner of these C*/
			
			list.add(cloudlet[i]);
		}

		return list;
	}
	
		
	
	/** Random integer function.	
	 * @param aStart
	 * @param aEnd
	 * @param aRandom
	 * @return
	 */
	
	
	private static int showRandomInteger(int aStart, int aEnd, Random aRandom){
	    if (aStart > aEnd) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }

	    long range = (long)aEnd - (long)aStart + 1;		/* Get the range, casting to long to avoid overflow problems. */
	    long fraction = (long)(range * aRandom.nextDouble());
	    int randomNumber =  (int)(fraction + aStart);    
	    
	    return randomNumber;
	  }
	
	
	
	
	/** Calculate a deadline for a task with respect to the Base Station range.
	 * @param a
	 * @param dc
	 * @return
	 */

		
	public static double Deadline(Cloudlet a,ETCmatrix b,double slack) {
		
		double comDelay = (a.getCloudletFileSize()/54000000);
		double arrivalTime = a.getArrivalTime();
		double avgMuAll = (b.getMu(a.getTaskType(), 0)+b.getMu(a.getTaskType(),1))/ 2 ;
		double deadline =  avgMuAll+ slack + arrivalTime + comDelay;
		a.setDeadline(deadline);
		return deadline;
	}
	
	/** Round function.
	 * @param value
	 * @param places
	 * @return
	 */
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	
	private static double getConvolveProbability(double mu, double sigma, double deadLine) {
		
		try {
		NormalDistribution distr = new NormalDistribution(mu, sigma);
		return distr.cumulativeProbability(deadLine);
		}catch(NotStrictlyPositiveException exc) {
			return 0.0;
		}
	
	}
	
	/**Load Balancer implementation.
	 * @param arrivingCloudlets
	 * @param vmMips1
	 * @param vmMips2
	 * @param vmMips3
	 * @param broker1
	 * @param broker2
	 * @param broker3
	 * @param matrix
	 * @param trial
	 * @throws Exception 
	 */
	private static void coordinatorMCT(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2, Datacenter edge, Datacenter cloud,DatacenterBroker edgeBroker, DatacenterBroker cloudBroker, ETCmatrix matrix,ETTmatrix ettmatrix, int trial) throws Exception {
		
		/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		
		double loseDeadline = 0;
		
		double slacktime1 = 0.45+loseDeadline;
		double slacktime2 = 0.79+loseDeadline;
		
		noTaskNotAllocated = 0;			

		
		double temAvgCompletionTime[] = new double[3];
		for(int y=0;y<3;y++) {
			temAvgCompletionTime[y] = 0.0;
		}

		double baseStationProbability[] = new double[3];
		for(int b=0;b<3;b++) {
			baseStationProbability[b] = 0.0;
		}
		
		double Coefficient = 0.001;
		
		double check;
		int checkBS;

		double convMu;
		double convSigma;

		
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(edgeBroker.getId());
				batchQueBS1.add(cloudlet);
			}
			
			edgeBroker.submitCloudletList(batchQueBS1); 
	
		}
		else if(trial == 1) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(cloudBroker.getId());
				batchQueBS2.add(cloudlet);
			}
			cloudBroker.submitCloudletList(batchQueBS2);
		}
		else {
			
			for(Cloudlet cloudlet: arrivingCloudlets){
				check = -100;
				checkBS = -1;
				
				for(int p=0;p<2;p++) {
					if (p == 0) {
						//temProbability[p] = (matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime2)-cloudlet.getArrivalTime()))-baseStationProbability[p];
						//temAvgCompletionTime[p] = matrix.getMu(cloudlet.getTaskType(), p)-baseStationProbability[p];
						double avgCompletionTime = matrix.getMu(cloudlet.getTaskType(), p);
						//double downLinkDelayEdge = cloudlet.getCloudletFileSize() / edge.getBandWidth();
						temAvgCompletionTime[p] = ( Deadline(cloudlet, matrix, slacktime2)- avgCompletionTime ) -baseStationProbability[p];
					
					}
					else if (p == 1){
						convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
						convSigma = matrix.getSigma(cloudlet.getTaskType(), p)+ ettmatrix.getSigma(cloudlet.getTaskType(), p);
						//temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime1)-cloudlet.getArrivalTime())-baseStationProbability[p];
						
						//double downLinkDelayCloud = cloudlet.getCloudletFileSize() / edge.getBandWidth();
						temAvgCompletionTime[p] = ( Deadline(cloudlet, matrix, slacktime1) - convMu ) -baseStationProbability[p];
					}
					
					if(temAvgCompletionTime[p] > check) {
							
							check = temAvgCompletionTime[p];
							checkBS = p;
					}
//					else if(temAvgCompletionTime[p] == check) {
//						if(checkBS == 0) {
//							break;
//						}
//						double prev = matrix.getSigma(cloudlet.getTaskType(), checkBS);
//						double curr = matrix.getSigma(cloudlet.getTaskType(), p);
//						
//						if(curr < prev) {
//							check = temAvgCompletionTime[p];
//							checkBS = p;	
//						}
//					}
					
					}
				
				if(checkBS == 0 && cloudlet.getTaskType() == 1) { //
					cloudlet.setUserId(edgeBroker.getId());
					batchQueBS1.add(cloudlet);
					numTaskAllocatedBS1++;
					baseStationProbability[checkBS]+=  80.0; // base station 0
				}
				else if (checkBS == 1) {
					cloudlet.setUserId(cloudBroker.getId());
					batchQueBS2.add(cloudlet);
					numTaskAllocatedBS2++;
					baseStationProbability[1]+=  40.0;
				}
				else {
					noTaskNotAllocated++;
				}
				}
			
			edgeBroker.submitCloudletList(batchQueBS1); 
			cloudBroker.submitCloudletList(batchQueBS2); 
			
		
		}
		
	}
	
	
	/**Load Balancer implementation.
	 * @param arrivingCloudlets
	 * @param vmMips1
	 * @param vmMips2
	 * @param vmMips3
	 * @param broker1
	 * @param broker2
	 * @param broker3
	 * @param matrix
	 * @param trial
	 * @throws Exception 
	 */
	private static void loadBalancerTaskType(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2, DatacenterBroker edgeBroker, DatacenterBroker cloudBroker, ETCmatrix matrix,ETTmatrix ettmatrix, int trial) throws Exception {
		
		/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		
		double loseDeadline = 0;
		
		double slacktime1 = 0.45+loseDeadline;
		double slacktime2 = 0.79+loseDeadline;
		
		noTaskNotAllocated = 0;			

		
		double temAvgCompletionTime[] = new double[3];
		for(int y=0;y<3;y++) {
			temAvgCompletionTime[y] = 0.0;
		}

		double baseStationProbability[] = new double[3];
		for(int b=0;b<3;b++) {
			baseStationProbability[b] = 0.0;
		}
		
		double Coefficient = 0.001;
		
		double check;
		int checkBS;

		double convMu;
		double convSigma;

		
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(edgeBroker.getId());
				batchQueBS1.add(cloudlet);
			}
			
			edgeBroker.submitCloudletList(batchQueBS1); 
	
		}
		else if(trial == 1) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(cloudBroker.getId());
				batchQueBS2.add(cloudlet);
			}
			cloudBroker.submitCloudletList(batchQueBS2);
		}
		else {
			
			for(Cloudlet cloudlet: arrivingCloudlets){
				check = 999999;
				checkBS = -1;
				
				for(int p=0;p<2;p++) {
					if (p == 0) {
						//temProbability[p] = (matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime2)-cloudlet.getArrivalTime()))-baseStationProbability[p];
						temAvgCompletionTime[p] = matrix.getMu(cloudlet.getTaskType(), p)-baseStationProbability[p];
					
					}
					else if (p == 1){
						convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
						convSigma = matrix.getSigma(cloudlet.getTaskType(), p)+ ettmatrix.getSigma(cloudlet.getTaskType(), p);
						//temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime1)-cloudlet.getArrivalTime())-baseStationProbability[p];
						
						
						temAvgCompletionTime[p] = convMu-baseStationProbability[p];
					}
					if(temAvgCompletionTime[p] < check) {
							
							check = temAvgCompletionTime[p];
							checkBS = p;
					}
					else if(temAvgCompletionTime[p] == check) {
						if(checkBS == 0) {
							break;
						}
						double prev = matrix.getSigma(cloudlet.getTaskType(), checkBS);
						double curr = matrix.getSigma(cloudlet.getTaskType(), p);
						
						if(curr < prev) {
							check = temAvgCompletionTime[p];
							checkBS = p;	
						}
					}
					}
				
				if(checkBS == 0 && cloudlet.getTaskType() == 1) {
					cloudlet.setUserId(edgeBroker.getId());
					batchQueBS1.add(cloudlet);
					numTaskAllocatedBS1++;
					baseStationProbability[checkBS]+=  0.001; // base station 0
				}
				else if (checkBS == 1 && (cloudlet.getTaskType() == 1 || cloudlet.getTaskType() == 2)) {
					cloudlet.setUserId(cloudBroker.getId());
					batchQueBS2.add(cloudlet);
					numTaskAllocatedBS2++;
					baseStationProbability[1]+=  0.006;
				}
				else {
					noTaskNotAllocated++;
				}
				}
			
			edgeBroker.submitCloudletList(batchQueBS1); 
			cloudBroker.submitCloudletList(batchQueBS2); 
			
		
		}
		
	}
	
	
	/**Load Balancer implementation.
	 * @param arrivingCloudlets
	 * @param vmMips1
	 * @param vmMips2
	 * @param vmMips3
	 * @param broker1
	 * @param broker2
	 * @param broker3
	 * @param matrix
	 * @param trial
	 * @throws Exception 
	 */
	private static void coordinatorTaskType(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2, DatacenterBroker edgeBroker, DatacenterBroker cloudBroker, ETCmatrix matrix,ETTmatrix ettmatrix, int trial) throws Exception {
		
		/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		
		noTaskNotAllocated = 0;			


		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(edgeBroker.getId());
				batchQueBS1.add(cloudlet);
			}
			
			edgeBroker.submitCloudletList(batchQueBS1); 
	
		}
		else if(trial == 1) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(cloudBroker.getId());
				batchQueBS2.add(cloudlet);
			}
			cloudBroker.submitCloudletList(batchQueBS2);
		}
		else {
			
			for(Cloudlet cloudlet: arrivingCloudlets){
				
				if(cloudlet.getTaskType() == 1) {
					cloudlet.setUserId(edgeBroker.getId());
					batchQueBS1.add(cloudlet);
					numTaskAllocatedBS1++;
				}
				else{
					cloudlet.setUserId(cloudBroker.getId());
					batchQueBS2.add(cloudlet);
					numTaskAllocatedBS2++;
				}
			
			edgeBroker.submitCloudletList(batchQueBS1); 
			cloudBroker.submitCloudletList(batchQueBS2);
			batchQueBS1.clear();
			batchQueBS2.clear();
			}
		}
		
	}
	
	
     /**
	 * Creates main() to run the example
	 * @throws IOException 
	 */
	
	
	public static void main(String[] args) throws IOException {
		Log.printLine("Starting Simulation for Smart Oil task processing...");
		numTasksAdmitted=0;
		int number = 0;
		int arrivingTasks = 0;
		int lbOption = -1;
		long userSeed=0;
		String SchPolicy=null;
		taskDstrList = new ArrayList<Cloudlet>();
		
		
		FileWriter fw = new FileWriter("ResultSmartOil.txt",true);
		PrintWriter printWriter = new PrintWriter(fw);
		printWriter.println();
		printWriter.println("Starting Simulation for Smart Oil task processing...");
		printWriter.println();
		
		try{
		
			File file = new File("oilinput.txt"); 									/* Read from the input file. */
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			try {
				String NO_Trail = br.readLine();
				number = Integer.parseInt(NO_Trail);
				String secondStr = br.readLine();
				arrivingTasks = Integer.parseInt(secondStr);
				String LB = br.readLine();
				//lbOnOff = Boolean.parseBoolean(LB);								/* Load balancer on/off option. */
				lbOption = Integer.parseInt(LB);
				SchPolicy = br.readLine();										/* Scheduler policy. */
				String seed = br.readLine();
				userSeed = Long.parseLong(seed);
				
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
		printWriter.println("Load Balancer Status :"+lbOption);
		printWriter.println("Scheduler Policy :"+SchPolicy);
		printWriter.println("User Seed :"+userSeed);
		printWriter.println();
		
	
		/* Trial loop */ 
		
	    for(int i = 0;i <number;i++) {
			
			try {
			printWriter.println("Trial No : "+ i);
			
			/* First step: initialize the CloudSim package. It should be called before creating any entities.*/
			
			int num_user =1;   												/* Number of grid users.*/
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  									/* Mean trace events.*/
			CloudSim.init(num_user, calendar, trace_flag);
			
			/* Second step: create 3 Base Station.*/ 
			
			Datacenter edgeBS = createEdgeBS("EdgeBS",1,0,60,54000000);	/* Base Station x with coordinates 0 and the range of 60 meters. */
			
			Datacenter cloudServer = createDatacenter("Cloud",1,5,65,2000000);	/* Base Station x  with coordinates 5 and the range of 65 meters. */
			
			/* Third step: Create Brokers.*/
			DatacenterBroker edgeBroker = createBroker("edge");
			int vmMipsEdge = 2000;
			vmlist1 = createVM_N(edgeBroker.getId(), 8,vmMipsEdge, 1);
			edgeBroker.submitVmList(vmlist1);
			edgeBroker.setSchedulerPolicy(SchPolicy);
			
			
			DatacenterBroker cloudBroker = createBroker("cloud");
			int vmMipsCloud = 10000;
			vmlist2 = createVM_N(cloudBroker.getId(), 16,vmMipsCloud, 1001);
			cloudBroker.submitVmList(vmlist2);
			cloudBroker.setSchedulerPolicy(SchPolicy);
			
		
			/* End of broker creation - Razin.*/
		
			if(i>1) {
				
				userSeed = userSeed +20000;
				
			}
			
			numTasksAdmitted = arrivingTasks;
			cloudletList3 = createCloudlet(edgeBroker.getId(),numTasksAdmitted,3500,12500,30000,userSeed,1); // arrival1.dat file 
			
			cloudletList4 = createCloudlet(edgeBroker.getId(),initialWorkload,3500,12500,40000,userSeed, 2); // arrival2.dat file 
			cloudletList5 = createCloudlet(cloudBroker.getId(),initialWorkload,3500,12500,50000,userSeed, 3); // arrival3.dat file 
			
			
			// Generating three workload for 3 Base Station
			numTaskAllocatedBS1 = 0;
			numTaskAllocatedBS2 = 0;
			//numTaskAllocatedBS3 = 0;
			
				
    
    		if (lbOption == 1) { // urgency coordinator
     			if(i == 0) {
     				
     				coordinatorMCT(cloudletList4, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);

     			}
     			else if(i == 1) {
     				coordinatorMCT(cloudletList5, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     				
     			}
     			else {
     				
     				double loseDeadline = 0;
     				for(Cloudlet cloudlet:cloudletList4) { // for setting the deadline
         				Deadline(cloudlet, matrix, 0.79+loseDeadline);
         				}
         			for(Cloudlet cloudlet:cloudletList5) { // for setting the deadline
         				Deadline(cloudlet, matrix, 0.45+loseDeadline );
         					
         				}
         			for(Cloudlet cloudlet:cloudletList3) { // for setting the deadline
         				Deadline(cloudlet, matrix, 0.79);
         					
         				}
     		
         			edgeBroker.submitCloudletList(cloudletList4); 
         			cloudBroker.submitCloudletList(cloudletList5); 
         			coordinatorMCT(cloudletList3, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     			}
     			
			}
    		else if(lbOption == 2) { // task type coordinator
    			if(i == 0) {
     				
    				coordinatorTaskType(cloudletList4, vmMipsEdge, vmMipsCloud, edgeBroker, cloudBroker, matrix,ettMatrix,i);

     			}
     			else if(i == 1) {
     				coordinatorTaskType(cloudletList5, vmMipsEdge, vmMipsCloud, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     				
     			}
     			else {
     				
     				double loseDeadline = 0;
     				for(Cloudlet cloudlet:cloudletList4) { // for setting the deadline
         				Deadline(cloudlet, matrix, 0.79+loseDeadline);
         				}
         			for(Cloudlet cloudlet:cloudletList5) { // for setting the deadline
         				Deadline(cloudlet, matrix, 0.45+loseDeadline );
         					
         				}
         			for(Cloudlet cloudlet:cloudletList3) { // for setting the deadline
         				Deadline(cloudlet, matrix, 0.79+loseDeadline);
         					
         				}
     		
         			edgeBroker.submitCloudletList(cloudletList4); 
         			cloudBroker.submitCloudletList(cloudletList5); 
         			coordinatorTaskType(cloudletList3, vmMipsEdge, vmMipsCloud, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     			}
    			
    		}
    		else if(lbOption == 3) { // submit everything to cloud (Baseline approach)
    			if(i == 0) {
     				
     				coordinatorMCT(cloudletList4, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);

     			}
     			else if(i == 1) {
     				coordinatorMCT(cloudletList5, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     				
     			}
     			else {
     				
     			double cloudLooseness = 0;	
     				
     				
    			for(Cloudlet cloudlet:cloudletList4) { // for setting the deadline
     				Deadline(cloudlet, matrix, (0.79+cloudLooseness));
     				}
     			for(Cloudlet cloudlet:cloudletList3) { // for setting the deadline
     				Deadline(cloudlet, matrix, (0.45));
     					
     				}
     			for(Cloudlet cloudlet:cloudletList5) { // for setting the deadline
     				double downlinkDelay5 = cloudlet.getCloudletFileSize()/ 54000000;
     				cloudlet.setDeadline(Deadline(cloudlet, matrix, (0.45)) - downlinkDelay5);
     				Deadline(cloudlet, matrix, (0.45+cloudLooseness));
     				}
    			
     			edgeBroker.submitCloudletList(cloudletList4);
     			
     			List<Cloudlet> batchQueCloud = new ArrayList<Cloudlet>();
     			
     			for(Cloudlet cloudlet:cloudletList3) {
     				cloudlet.setUserId(cloudBroker.getId());
     				double transferDelay = (cloudlet.getCloudletFileSize()/2000000);
     				cloudlet.setDeadline(Deadline(cloudlet, matrix, (0.45+cloudLooseness)) - transferDelay);
     				//Deadline(cloudlet, matrix, (0.45));
    				batchQueCloud.add(cloudlet);
    			}
    			cloudBroker.submitCloudletList(batchQueCloud);
     			cloudBroker.submitCloudletList(cloudletList5);
     			
     			
     			}
    			
    		}
     		else { // submit everything to edge
     			if(i==0) {
     				coordinatorMCT(cloudletList4, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     			}
     			else if(i==1) {
     				coordinatorMCT(cloudletList5, vmMipsEdge, vmMipsCloud,edgeBS,cloudServer, edgeBroker, cloudBroker, matrix,ettMatrix,i);
     			
     			}
     			else {
    
     			double nl_loose = 0;
     			for(Cloudlet cloudlet:cloudletList4) { // for setting the deadline
     				Deadline(cloudlet, matrix, (0.79 + nl_loose));
     				}
     			for(Cloudlet cloudlet:cloudletList3) { // for setting the deadline
     				Deadline(cloudlet, matrix, (0.79 + nl_loose));
     					
     				}
     			for(Cloudlet cloudlet:cloudletList5) { // for setting the deadline
     				Deadline(cloudlet, matrix, (0.45 + nl_loose));
     				}
     			
     			edgeBroker.submitCloudletList(cloudletList4); 
     			edgeBroker.submitCloudletList(cloudletList3);
     			cloudBroker.submitCloudletList(cloudletList5); 
     			}
     				
     		}
			
    		
    		double lastArrivalTime = 0.0;
    		for(Cloudlet cloudlet:cloudletList3) {
    			
    			if(cloudlet.getArrivalTime()>lastArrivalTime) {
    				lastArrivalTime = cloudlet.getArrivalTime();
    			}
    		}
    		
     		
		   /* Fifth step: Start the simulation. */
			
			CloudSim.startSimulation();
			
			
			/* Final step: Print results when simulation is over. */
			
			List<Cloudlet> newList = edgeBroker.getCloudletReceivedList();
			newList.addAll(cloudBroker.getCloudletReceivedList());
			//newList.addAll(broker3.getCloudletReceivedList());
			
		
			if(i==0) {
				
				getDistribution(cloudletList4);
				getETTDistribution(cloudletList4, edgeBS, cloudServer);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
				
			}
			else if(i==1) {
				getDistribution(cloudletList5);
				getETTDistribution(cloudletList5, edgeBS, cloudServer);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
			}
			else {
				getDistribution(cloudletList3);
				getETTDistribution(cloudletList3, edgeBS, cloudServer);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
			}
		
			
			CloudSim.stopSimulation();
			
			printCloudletList(newList,printWriter,numTasksAdmitted);
			
			
			double simulationTime = 0.0;
			
			for(Cloudlet cloudlet:newList) {
				
				if(cloudlet.getFinishTime() > simulationTime) {
					simulationTime = cloudlet.getFinishTime();
				}
				
			}
					
			
		
			
			allTaskNotAllocated.add(noTaskNotAllocated);
			printWriter.println();
		
			printWriter.println(" All counter array ::"+ allCounters.toString());
			printWriter.println();
			printWriter.println(" allTaskNotAllocated array ::"+ allTaskNotAllocated.toString());

	
			System.out.println(" Task not allocated Array ::"+ allTaskNotAllocated.toString());
			
			System.out.println("$$$$$$$ Type 1 , Base Station 0 : " + matrix.getSigma(1,0) + " " + matrix.getMu(1, 0)); 
			System.out.println("$$$$$$$ Type 1 , Base Station 1 : " + matrix.getSigma(1,1) + " " + matrix.getMu(1, 1));
			//System.out.println("$$$$$$$ Type 1 , Base Station 2 : " + matrix.getSigma(1,2) + " " + matrix.getMu(1, 2));
			
			System.out.println("$$$$$$$ ETT matrix Type 1 , Base Station 0 : " + ettMatrix.getSigma(1,0) + " " + ettMatrix.getMu(1, 0));
			System.out.println("$$$$$$$ ETT matrix Type 1 , Base Station 1 : " + ettMatrix.getSigma(1,1) + " " + ettMatrix.getMu(1, 1));
			//System.out.println("$$$$$$$ ETT matrix Type 1 , Base Station 2 : " + ettMatrix.getSigma(1,2) + " " + ettMatrix.getMu(1, 2));
			
			printWriter.println();
			printWriter.println("$$$$$$$ Type 1 , Base Station 0 : " + matrix.getSigma(1,0) + " " + matrix.getMu(1, 0)); 
			printWriter.println("$$$$$$$ Type 1 , Base Station 1 : " + matrix.getSigma(1,1) + " " + matrix.getMu(1, 1));
			//printWriter.println("$$$$$$$ Type 1 , Base Station 2 : " + matrix.getSigma(1,2) + " " + matrix.getMu(1, 2));
						
			
			System.out.println("$$$$$$$ Type 2 , Base Station 0 : " + matrix.getSigma(2,0) + " " + matrix.getMu(2, 0));
			System.out.println("$$$$$$$ Type 2 , Base Station 1 : " + matrix.getSigma(2,1) + " " + matrix.getMu(2, 1));
			//System.out.println("$$$$$$$ Type 2 , Base Station 2 : " + matrix.getSigma(2,2) + " " + matrix.getMu(2, 2));
			
			System.out.println();
			
			System.out.println("$$$$$$$ ETT matrix Type 2 , Base Station 0 : " + ettMatrix.getSigma(2,0) + " " + ettMatrix.getMu(2, 0));
			System.out.println("$$$$$$$ ETT matrix Type 2 , Base Station 1 : " + ettMatrix.getSigma(2,1) + " " + ettMatrix.getMu(2, 1));
			//System.out.println("$$$$$$$ ETT matrix Type 2 , Base Station 2 : " + ettMatrix.getSigma(2,2) + " " + ettMatrix.getMu(2, 2));
					
			printWriter.println("$$$$$$$ Type 2 , Base Station 0 : " + matrix.getSigma(2,0) + " " + matrix.getMu(2, 0)); 
			printWriter.println("$$$$$$$ Type 2 , Base Station 1 : " + matrix.getSigma(2,1) + " " + matrix.getMu(2, 1));
			printWriter.println();
			printWriter.println("$$$$$$$ Type 3 , Base Station 0 : " + matrix.getSigma(3,0) + " " + matrix.getMu(3, 0)); 
			printWriter.println("$$$$$$$ Type 3 , Base Station 1 : " + matrix.getSigma(3,1) + " " + matrix.getMu(3, 1));
			
			
			
			
			//printWriter.println("$$$$$$$ Type 2 , Base Station 2 : " + matrix.getSigma(2,2) + " " + matrix.getMu(2, 2));
			printWriter.println();
			printWriter.println("$$$$$$$ ETT matrix Type 1 , Base Station 0 : sd:" + ettMatrix.getSigma(1,0) + " mu :" + ettMatrix.getMu(1, 0));
			printWriter.println("$$$$$$$ ETT matrix Type 1 , Base Station 1 : sd:" + ettMatrix.getSigma(1,1) + " mu :" + ettMatrix.getMu(1, 1));
			//printWriter.println("$$$$$$$ ETT matrix Type 1 , Base Station 2 : sd:" + ettMatrix.getSigma(1,2) + " mu :" + ettMatrix.getMu(1, 2));


			printWriter.println();
			printWriter.println("$$$$$$$ ETT matrix Type 2 , Base Station 0 : sd :" + ettMatrix.getSigma(2,0) + " mu :" + ettMatrix.getMu(2, 0));
			printWriter.println("$$$$$$$ ETT matrix Type 2 , Base Station 1 : sd :" + ettMatrix.getSigma(2,1) + " mu :" + ettMatrix.getMu(2, 1));
			//printWriter.println("$$$$$$$ ETT matrix Type 2 , Base Station 2 : sd :" + ettMatrix.getSigma(2,2) + " mu :" + ettMatrix.getMu(2, 2));

			
			//task type 3
			printWriter.println();
			printWriter.println("$$$$$$$ ETT matrix Type 3 , Base Station 0 : sd :" + ettMatrix.getSigma(3,0) + " mu :" + ettMatrix.getMu(3, 0));
			printWriter.println("$$$$$$$ ETT matrix Type 3 , Base Station 1 : sd :" + ettMatrix.getSigma(3,1) + " mu :" + ettMatrix.getMu(3, 1));

			
			
			
			System.out.println(" No of task not allocated ::::@@@#########  "+ noTaskNotAllocated);
			System.out.println(" Task Allocated to Edge :" + numTaskAllocatedBS1 + " Task Allocated to Cloud :" + numTaskAllocatedBS2);
			
			printWriter.println();
			printWriter.println(" No of task not allocated ::::@@@#########  "+noTaskNotAllocated);
			
			
			printWriter.println(" Task Allocated to Edge :" + numTaskAllocatedBS1 + " Task Allocated to Cloud :" + numTaskAllocatedBS2);
			Log.printLine("V2I task processing finished!");
			
			System.out.println("Simulation run for :"+lastArrivalTime +" seconds");

			printWriter.println();
			printWriter.println("End of trail : "+ i);
			printWriter.println();
			printWriter.println("Simulation time :" + simulationTime);
			printWriter.println();
     		
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
			printWriter.println("The simulation has been terminated due to an unexpected error");
			
		}
		
	}
		
		//take allcounter array and provide the average
		
		double sizeOfArray = allCounters.size();
		double sum = 0;
		double result = 0;
		double checkMax=-1;
		double checkMin=200000;
		
		for(int x=2;x<sizeOfArray;x++)
		{
			sum = sum + allCounters.get(x);
			
		}
		
		result = (sum/(sizeOfArray-2));
		
		
		double sd = 0;
		for (int i = 2; i <sizeOfArray; i++)
		{
			
		    sd += Math.pow((allCounters.get(i) - result), 2) / (sizeOfArray-2);
		}
		double standardDeviation = Math.sqrt(sd);
		
		
		
		String str = null;
		if(lbOption == 1) {
			str = "with certainty co-ordinator ("+SchPolicy+")";
		}
		else if(lbOption == 2){
			str = " Task Type ("+SchPolicy+")";
		}
		else {
			str = " CloudOnly ("+SchPolicy+")";
		}

		
		FileWriter flwrt = new FileWriter("/home/c00303945/ResearchWork/Spring_2018/Oil_Field_Project/Results/Experiments19th.txt",true);
		PrintWriter prttWrt = new PrintWriter(flwrt);
		
		prttWrt.println();
		prttWrt.println("Number of tasks submitted "+arrivingTasks);
		prttWrt.println("Initial Over Subscription Workload : " + initialWorkload);
		prttWrt.println("All counter array ::"+ allCounters.toString()+ " "+ str); 
		prttWrt.println("Average of task missing deadline : " + result);
		prttWrt.println("Min : " + checkMin);
		prttWrt.println("Max : " + checkMax);
		prttWrt.println("Standard Deviation : "+standardDeviation);
		prttWrt.println("============================================================================================");
		prttWrt.close();
		
		
		System.out.println("Average of task missing deadline : " + result);
		printWriter.println("Number of tasks "+arrivingTasks);
		printWriter.println("All counter array ::"+ allCounters.toString()+ " "+ str); 
		printWriter.println("Average of task missing deadline : " + result);
		printWriter.println("Min : " + checkMin);
		printWriter.println("Max : " + checkMax);
		printWriter.println("Standard Deviation : "+standardDeviation);
		
		
		
		//}
		printWriter.println("V2I task processing finished!");
		printWriter.println("********************************************************************************************************");
		printWriter.close();
		
		
	}
	
	

	private static Datacenter createEdgeBS(String name, int hostNumber, double x,double range,long linkBW){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 5000; // this is the mips for each core of host of datacenter

		// 3. Create PEs and add these into the list.
		//for a five-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(4, new PeProvisionerSimple(mips)));// need to store Pe id and MIPS Rating
		peList1.add(new Pe(5, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(6, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(7, new PeProvisionerSimple(mips)));
		
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
		//long linkBandWidth = 54000000;
		try {
			datacenter = new Datacenter(name, x , linkBW, range,characteristics, new VmAllocationPolicySimple(hostList), storageList, 100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}
	
	private static Datacenter createDatacenter(String name, int hostNumber, double x,double range,long linkBW){

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
		//peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
		//peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(4, new PeProvisionerSimple(mips)));// need to store Pe id and MIPS Rating
		//peList1.add(new Pe(5, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(6, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(7, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(8, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(9, new PeProvisionerSimple(mips)));
		
		for(int p= 0;p<32;p++) {
			
			peList1.add(new Pe(p, new PeProvisionerSimple(mips)));
		}
		
		
		
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
		//long linkBandWidth = 54000000;
		try {
			datacenter = new Datacenter(name, x , linkBW, range,characteristics, new VmAllocationPolicySimple(hostList), storageList, 100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
}	
	
	
	
private static void getETTDistribution(List<Cloudlet> list, Datacenter dc0, Datacenter dc1) {
		
		Cloudlet cloudlet;
		int size = list.size();
		
			
		for(int i = 0; i < size; i++) {
			double transferTime = 0.00;
			cloudlet = list.get(i);
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				
				String key = cloudlet.getTaskType() + "." + (cloudlet.getResourceId()-2);
				
				if((cloudlet.getResourceId()-2)==0)
				{
					transferTime = 2*((double)cloudlet.getCloudletFileSize()/dc0.getBandWidth());
				}
				else if((cloudlet.getResourceId()-2)==1) {
				
					transferTime = 2*((double)cloudlet.getCloudletFileSize()/dc1.getBandWidth()) + 0.28; // adding propagation delay for cloud
				}
				
				
			transferTimes.put(key,transferTime);	
			}
		}
		
		
		
		for(String taskTbaseST : transferTimes.keySet())	{
			
			List<Double> times = transferTimes.get(taskTbaseST);
			double sum = 0;
			double sqsum = 0;
			for(Double time: times) {
				sum += time;
			}
			double mu = sum/times.size();
			
			try {
				if (mu == sum) {
					 double sigma = 0.0;
					 NormalDistribution distr = new NormalDistribution(mu, sigma);
						trTimes.put(taskTbaseST, distr);
				}
				
				else {
					for(Double time: times) {
						sqsum += Math.pow((time-mu), 2);
					}
					double sigma = Math.sqrt(sqsum/(times.size()-1));
					NormalDistribution distr = new NormalDistribution(mu, sigma);
					trTimes.put(taskTbaseST, distr);
				}
				}catch( NotStrictlyPositiveException exp) {
				}
			}			

		}

	/** A method that returns a hashmap with the distributions for all the tasks just ran.
	 * @param list
	 * @return
	 */

	private static void getDistribution(List<Cloudlet> list) {
		
		Cloudlet cloudlet;
		int size = list.size();

		//long timeMillis = System.currentTimeMillis(); // current time in miliseconds
        //long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);// current time in seconds
		
			
		for(int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				String key = cloudlet.getTaskType() + "." + (cloudlet.getResourceId()-2);
				
				double completionTime = cloudlet.getFinishTime() - cloudlet.getArrivalTime(); // current completion time
				// 
				executionTimes.put(key, completionTime);	
			}
		}
		
		for(String taskTbaseST : executionTimes.keySet())	{
			
			List<Double> times = executionTimes.get(taskTbaseST);
			double sum = 0;
			double sqsum = 0;
			for(Double time: times) {
				sum += time;
			}
			double mu = sum/times.size();
			try {
			if (mu == sum) {
				 double sigma = 0.0;
				 NormalDistribution distr = new NormalDistribution(mu, sigma);
					distributions.put(taskTbaseST, distr);
			}
			
			else {
				for(Double time: times) {
					sqsum += Math.pow(time-mu, 2);
				}
				double sigma = Math.sqrt(sqsum/(times.size()-1));
				NormalDistribution distr = new NormalDistribution(mu, sigma);
				distributions.put(taskTbaseST, distr);
			}
			}catch( NotStrictlyPositiveException exp) {
				}
			}
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
	private static void printCloudletList(List<Cloudlet> list, PrintWriter pw, int numOfTaskAdmitted) throws IOException {
		int size = list.size();
		Cloudlet cloudlet;
		
		int counter = 0;
		String indent = "    ";
		Log.printLine();
		pw.println();//file write
		Log.printLine("========== OUTPUT ==========");
		pw.println("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent + indent+ "VM ID" + indent + indent+"  "+ "Execution Time"+indent+indent +"Task Length(MI)"+indent+"Task Type"+indent+indent+indent+ "Start Time" + indent + "Finish Time"+indent+indent+indent+"Deadline"+indent+indent+"Completion Time"+indent+indent+"DeadlineMet"+indent+indent+"Arrival Time");
        
		pw.println("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent+indent+ "VM ID" + indent + indent+"  "+ "Execution Time"+indent+indent +"Task Length(MI)"+ indent+"Task Type"+indent+indent+indent+ "Start Time" + indent + "Finish Time"+indent+indent+indent+"Deadline"+indent+indent+"Completion Time"+indent+indent+"DeadlineMet"+indent+indent+"Arrival Time");
		
		
		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);
			
			pw.print(indent + cloudlet.getCloudletId() + indent + indent);

			String str=null;
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");
				pw.print("SUCCESS");
				
				
				//if(cloudlet.getCloudletId() >=30000 && cloudlet.getCloudletId()<= (30000+numTasksAdmitted)) {
				
					double downlinkdelay = cloudlet.getCloudletFileSize() / 54000000;
				
					if(cloudlet.getDeadline()< (cloudlet.getFinishTime() + downlinkdelay)) {
						str = "False";
						counter++;
					}
					else {
						str ="True";
					}
				//}
				
				Log.printLine( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent +indent+indent+indent +cloudlet.getCloudletLength()+ indent + indent +indent+indent+ cloudlet.getTaskType()+indent+indent+indent+ indent+dft.format(cloudlet.getExecStartTime())+ indent + indent+indent + dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+dft.format(cloudlet.getDeadline())+indent+indent+indent+indent+dft.format(cloudlet.getFinishTime() - cloudlet.getArrivalTime())+indent+indent+indent+indent+str+indent+indent+indent+cloudlet.getArrivalTime());
				
				
				pw.println( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent+indent+indent+indent + cloudlet.getCloudletLength()+ indent + indent +indent+indent+cloudlet.getTaskType()+indent +indent+indent+indent +dft.format(cloudlet.getExecStartTime())+ indent + indent + indent+dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+dft.format(cloudlet.getDeadline()-downlinkdelay)+indent+indent+indent+indent+dft.format(cloudlet.getFinishTime() - cloudlet.getArrivalTime())+indent+indent+indent+indent+str+indent+indent+indent+cloudlet.getArrivalTime());
				
			}
			
		}
		
		allCounters.add(counter);
		Log.printLine("Number of task missed deadline  : "+counter);
		pw.println("Number of task missed deadline  : "+counter);
		
		
		
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
