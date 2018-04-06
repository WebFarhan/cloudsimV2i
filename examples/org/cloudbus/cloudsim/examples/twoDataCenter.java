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
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
import org.cloudbus.cloudsim.distributions.normalDistr;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.util.MathArrays;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.sun.java_cup.internal.runtime.Scanner;
import com.sun.java_cup.internal.runtime.Symbol;


/**
 * An example simulation for efficient resource allocation
 * in V2I network within RSU/Base Stations
 * @author Razin Farhan Hussain
 * @author Anna Kovalenko
 */
public class twoDataCenter {

	
	private static List<Cloudlet> cloudletList1,cloudletList2,cloudletList3,cloudletList4,cloudletList5,cloudletList6,taskDstrList,workloadBS0,workloadBS1,workloadBS2;
	private static List<Cloudlet> overSubsList1,overSubsList2;
	
	private static int W1_SEED = 84500,W2_SEED = 95000,W3_SEED = 90950;
	
	private static List<List<Cloudlet>> workloadList;
	//private static List<VTasks> taskList;
	private static ArrayList<Integer> allCounters = new ArrayList<>();
	private static ArrayList<Integer> allTaskNotAllocated = new ArrayList<>();
	private static List<Vm> vmlist1, vmlist2, vmlist3, vmlist4;
	private static ETCmatrix matrix;
	private static ETTmatrix ettMatrix;
	private static HashMap<String, NormalDistribution> distributions = new HashMap<>();
	private static HashMap<String, NormalDistribution> trTimes = new HashMap<>();
	private static ListMultimap<String, Double> executionTimes = ArrayListMultimap.create();
	private static ListMultimap<String, Double> transferTimes = ArrayListMultimap.create();
	private static List<List<Vm>> VCs;
	private static int noTaskNotAllocated, taskMissDBS1, taskMissDBS2;
	public static int taskTypeNum = 3; 										 /*the number of task types*/
	public static int dataCenterNum = 3;										 /*the number of data centers*/
	public static Properties prop = new Properties();
	private static int numTaskAllocatedBS1, numTaskAllocatedBS2, numTaskAllocatedBS3;
	private static int allocatedTaskBS1,allocatedTaskBS2,allocatedTaskBS3;
	
	private static double[] arrList1,arrList2;

	
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
        //int mips = 2000;
        long bw = 1000;
        int pesNumber = 1; 							/*Number of CPUs*/
        String vmm = "Xen"; 							/*VMM Name*/
        //int hmips = 100;
        /*Create Virtual Machines*/
        
        Vm[] vm = new Vm[vms];
        
        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
            //hmips= hmips+500;
        }
        
        return list;
    }
	
	private static int gaussianMiGenerator(double sd,double mean, Random r,double mincap,double maxcap) {
		
		//Random r = new Random(seed);
	   //double sd=100,mean=2000;
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
		long fileSize = 34000000;
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
	
			//deadline = showRandomDouble(0.4, 1.5);
			priority = Math.pow((1/Math.E),deadline);
			length = gaussianMiGenerator(400, 1500,rObj,START,END);
			
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
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 20000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,arrList[i]);
			cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
			list.add(cloudlet[i]);
			//seed++;
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
		
	   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();		/* Creates a container to store Cloudlets*/
	   		/*Task (Cloudlets) parameters*/
		
		long length; 												/* MI of the Cloudlet */
		long fileSize = 64000000;
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
	
			//deadline = showRandomDouble(0.4, 1.5);
			priority = Math.pow((1/Math.E),deadline);
			length = gaussianMiGenerator(400, 1500,rObj,START,END);
			
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
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 20000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,0.0);
			cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
			
			list.add(cloudlet[i]);
			//seed++;
		}

		return list;
	}
	
	
	/** Create tasks (Cloudlets) for the base station 1
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
	
	private static List<Cloudlet> createCloudletForBS1(int userId, int cloudlets, int START, int END, int idShift, long seed) throws NumberFormatException, IOException{
		
	   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();		/* Creates a container to store Cloudlets*/
	   double[] arrList = new double[5700];
		
       File file = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival2.dat");
        
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
		long fileSize = 100;
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
	
			//deadline = showRandomDouble(0.4, 1.5);
			priority = Math.pow((1/Math.E),deadline);
			length = gaussianMiGenerator(400, 1500,rObj,START,END);
			
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
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 20000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,0.0);// for base station 1 creating over subscription
			cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
			list.add(cloudlet[i]);
			//seed++;
		}

		return list;
	}
	 
	/** Create tasks (Cloudlets) for the base station 2
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
	
	private static List<Cloudlet> createCloudletForBS2(int userId, int cloudlets, int START, int END, int idShift, long seed) throws NumberFormatException, IOException{
		
	   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();		/* Creates a container to store Cloudlets*/
	   double[] arrList = new double[5990];
		
       File file = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival3.dat");
        
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
		long fileSize = 100;
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
	
			//deadline = showRandomDouble(0.4, 1.5);
			priority = Math.pow((1/Math.E),deadline);
			length = gaussianMiGenerator(400, 1500,rObj,START,END);
			
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
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 20000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,0.0);
			cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
			list.add(cloudlet[i]);
			//seed++;
		}

		return list;
	}
	
	/** Create tasks (Cloudlets) for the base station 3
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
	
	private static List<Cloudlet> createCloudletForBS3(int userId, int cloudlets, int START, int END, int idShift, long seed) throws NumberFormatException, IOException{
		
	   LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();		/* Creates a container to store Cloudlets*/
	   double[] arrList = new double[6020];
		
       File file = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival4.dat");
        
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
		long fileSize = 100;
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
	
			//deadline = showRandomDouble(0.4, 1.5);
			priority = Math.pow((1/Math.E),deadline);
			length = gaussianMiGenerator(400, 1500,rObj,START,END);
			
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
					showRandomInteger(120,120,rObj),pesNumber, fileSize +showRandomInteger(15000, 20000,rObj), outputSize, utilizationModel, 
					utilizationModel, utilizationModel,0.0);
			cloudlet[i].setUserId(userId);		/* Setting the owner of these Cloudlets */
			list.add(cloudlet[i]);
			//seed++;
		}

		return list;
	}
	
	
	/** A function to generate tasks randomly for different task arriving situations. 

	 * This can be used for creating an oversibscribtion situation.
	 * @param aStart
	 * @param aEnd
	 * @param aRandom
	 * @return49, 48, 50, 3, 3, 3, 3, 3, 3]
	 */
	
	
	/*private static int generateTasksRandomly(int aStart, int aEnd, Random aRandom){

	    
		if (aStart > aEnd) {
		      throw new IllegalArgumentException("Start cannot exceed End.");
		    }
		
		    long range = (long)aEnd - (long)aStart + 1;			   Get the range, casting to long to avoid overflow problems
		    long fraction = (long)(range * aRandom.nextDouble());   Compute a fraction of the range, 0 <= frac < range 
		    int randomNumber =  (int)(fraction + aStart);    
		    
		    return randomNumber;
	 }	*/
		
	
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
	
	
	private static int randomMIGenerator(int Low,int High, Random r) {
		
		//Random r = new Random(seed);
		int Result = r.nextInt(High-Low) + Low;
		
		
		return Result;
	}
	
	
	
	
	public static double deadLineFirst(Cloudlet a,double slack, int mips) {
		
		double deadline = (a.getCloudletLength()/mips) +slack;
		
		a.setDeadline(deadline);
		
		return deadline;
		
	}
	
	
	/** Calculate a deadline for a task with respect to the Base Station range.
	 * @param a
	 * @param dc
	 * @return
	 */

		
	public static double Deadline(Cloudlet a,ETCmatrix b,double slack) {
		
		double uplinkDelay = (a.getCloudletFileSize()/54000000);
		double arrivalTime = a.getArrivalTime();
		
		double deadline = (b.getMu(a.getTaskType(), 0)+b.getMu(a.getTaskType(),1)+b.getMu(a.getTaskType(),2)/3+slack+arrivalTime+uplinkDelay);
		
		a.setDeadline(deadline);
		
		//System.out.println("------Deadline of task : "+a.getCloudletId()+ " is : "+(deadline*3600));
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
	
	/** Get the random double.
	 * @param aStart
	 * @param aEnd
	 * @return
	 */
	
	private static double showRandomDouble(double aStart, double aEnd){
	    if (aStart > aEnd) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }

	    Random r = new Random();
	    double randomValue = aStart + (aEnd - aStart) * r.nextDouble();
	    
	   return round(randomValue, 2);
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
	private static void loadBalancer(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3,Datacenter d0,Datacenter d1, Datacenter d2, ETCmatrix matrix,ETTmatrix ettmatrix, int trial) throws Exception {
		
		/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
		
		double loseDeadline = 0;
		
		double slacktime1 = 1.45;
		double slacktime2 = 1.52;
		double slacktime3 = 1.79;
		
		
		//double slacktime1 = 10.45;
		//double slacktime2 = 15.52;
		//double slacktime3 = 17.79;
		
		//double slacktime1 = 0.12;
		//double slacktime2 = 0.12;
		//double slacktime3 = 0.12;
		
		noTaskNotAllocated = 0;			
		taskMissDBS1 = 0;
		taskMissDBS2 = 0;
		
		
		double temProbability[] = new double[3];
		for(int y=0;y<3;y++) {
			temProbability[y] = 0.0;
		}
		
		
		double baseStationProbability[] = new double[3];
		for(int b=0;b<3;b++) {
			baseStationProbability[b] = 0.0;
		}
	
		double check;
		int checkBS;

		double convMu;
		double convSigma;
		double convSigma1;
		double convSigma2;
		
		
		//int convergValue = 385;
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker1.getId());
				
				System.out.println("broker 1 id : "+ broker1.getId());
				batchQueBS1.add(cloudlet);
				deadLineFirst(cloudlet, slacktime1,vmMips1);
			}
			
			broker1.submitCloudletList(batchQueBS1); 
	
		}
		else if(trial == 1) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker2.getId());
				System.out.println("broker 2 id : "+ broker2.getId());
				batchQueBS2.add(cloudlet);
				deadLineFirst(cloudlet, slacktime2,vmMips2);
			}
			broker2.submitCloudletList(batchQueBS2);
		}
		else if(trial == 2) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker3.getId());
				System.out.println("broker 2 id : "+ broker3.getId());
				batchQueBS3.add(cloudlet);
				deadLineFirst(cloudlet, slacktime3,vmMips3);
			}
			broker3.submitCloudletList(batchQueBS3);
		}
		else {
			
			for(Cloudlet cloudlet: arrivingCloudlets)
			{
				
				if(cloudlet.getTaskType() == 1) {
                    
					System.out.println("%%%%%%% Task type 1 %%%%%%%%%%%");
					check = -1.00;
					checkBS = -1;
					
					for(int p=0;p<3;p++) {
						if (p==0) {
						
							//if(baseStationProbability[p] < 0) {
							//	baseStationProbability[p] = 0.0;
							//}
							
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1))-baseStationProbability[p];
							
											
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							//if(baseStationProbability[p] < 0) {
							//	baseStationProbability[p] = 0.0;
							//}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2))-baseStationProbability[p];
							//baseStationProbability[p] = baseStationProbability[p]+temProbability[p];
						
													}
						else if(p==2) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							//if(baseStationProbability[p] < 0) {
							//	baseStationProbability[p] = 0.0;
							//}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3))-baseStationProbability[p];
						}
						
						//System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
		
					
					}// end of probability calculation
					
					
					// for loop to task allocation
					for(int index = 0;index<3;index++) {
						
						if(checkBS == index) {
							
							if(checkBS == 0 && temProbability[checkBS] > 0.0) { // receiving base station
							
							Deadline(cloudlet, matrix, slacktime1);
							cloudlet.setUserId(broker1.getId());
							batchQueBS1.add(cloudlet);
							
							numTaskAllocatedBS1++;
							allocatedTaskBS1++;

							baseStationProbability[checkBS]+=  0.001; // base station 0
							
							}
							else if (checkBS == 1 && temProbability[checkBS] > 0.0 ){ // base station 1
							
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 1
									
									Deadline(cloudlet, matrix, slacktime2);
									
									cloudlet.setUserId(broker2.getId());
									batchQueBS2.add(cloudlet);
									
									numTaskAllocatedBS2++;
									allocatedTaskBS2++;
									baseStationProbability[1]+=  0.09;
								}
												
								
							}// end of bs1 check
							else if(checkBS == 2 && temProbability[checkBS] > 0.0 ) {
								
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 2 (or 3rd BS)
									
									Deadline(cloudlet, matrix, slacktime3);
									
									cloudlet.setUserId(broker3.getId());
									batchQueBS3.add(cloudlet);
									
									numTaskAllocatedBS3++;
									allocatedTaskBS3++;
									baseStationProbability[2]+=  0.01;
								}
								
								
								
							}
							else {
								//no allocation
								noTaskNotAllocated++;
							}
							
							
						} // end of main if condition

						
					} // end of index for loop
					
					
				
//					
//					if(checkBS == 0 && temProbability[checkBS] > 0.0) {
//						Deadline(cloudlet, matrix, slacktime1);
//						cloudlet.setUserId(broker1.getId());
//						batchQueBS1.add(cloudlet);
//						
//						numTaskAllocatedBS1++;
//						allocatedTaskBS1++;
//
//						baseStationProbability[checkBS]+=  0.001;
//						
//						temProbability[checkBS] = -1;
//						
//						
//					}
//										
//						
//						
//					
//					
//					Arrays.sort(temProbability); // sorting the array in ascending order.
//					
//					
//					if(temProbability[0]!=-1) {// that means receiving bs is not the highest one
//						
//					
//						
//												
//					}
					
					
					
					
//					
//					if(checkBS == 0 && temProbability[checkBS] > 0.0) {
//						Deadline(cloudlet, matrix, slacktime1);
//						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
//						cloudlet.setUserId(broker1.getId());
//						batchQueBS1.add(cloudlet);
//						
//						numTaskAllocatedBS1++;
//						allocatedTaskBS1++;
//				
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS1)/convergValue));
//					
//						//if(numTaskAllocatedBS1%10==0 && numTaskAllocatedBS1 > 0) {
//							baseStationProbability[checkBS]+=  0.001;
//							//baseStationProbability[checkBS]+= slacktime1/numTaskAllocatedBS1;
//							
//						//}
//						
//							//baseStationProbability[checkBS]+=  0.1;
//						
//					}
//					else if(checkBS==1 && temProbability[checkBS]> 0.0)
//					{
//						Deadline(cloudlet, matrix, slacktime2);
//						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
//						cloudlet.setUserId(broker2.getId());
//						batchQueBS2.add(cloudlet);
//						
//						numTaskAllocatedBS2++;
//						allocatedTaskBS2++;
//						
//							//baseStationProbability[checkBS]=  1/(Math.sqrt((numTaskAllocatedBS2)/convergValue));
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS2)/convergValue));
//						
//						//if(numTaskAllocatedBS2%10==0 && numTaskAllocatedBS2>0) {
//						baseStationProbability[checkBS]+=  0.3;
//
//						//}
//					}
//					else if(checkBS==2 && temProbability[checkBS]> 0.0)
//					{
//						Deadline(cloudlet, matrix, slacktime3);
//						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
//						cloudlet.setUserId(broker3.getId());
//						batchQueBS3.add(cloudlet);
//						
//						numTaskAllocatedBS3++;
//						allocatedTaskBS3++;
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS3)/convergValue));
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS3)/convergValue));
//						//if(numTaskAllocatedBS3%10==0 && numTaskAllocatedBS3 > 0) {
//						baseStationProbability[checkBS]+=  0.01;
//						//}
//					}
//					else {
//						//cloudlet.setCloudletStatus(0);
//						noTaskNotAllocated++;
//					}
	
				}// end of task type 1 checking 
			   else if(cloudlet.getTaskType() == 2) {
				   System.out.println("%%%%%%% Task type 2 %%%%%%%%%%%");
				   
					check = -1.00;
					checkBS = -1;
					
	
					for(int p = 0; p < 3; p++) { // calculating the probability and store the highest probability
						if (p==0) {
							
							if(baseStationProbability[p] < 0) {
								baseStationProbability[p] = 0.0;
							}
							
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1))-baseStationProbability[p];
							
						
							
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							if(baseStationProbability[p] < 0) {
								baseStationProbability[p] = 0.0;
							}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2))-baseStationProbability[p];//Deadline(cloudlet,p,matrix,slacktime2)

						
						}
						else if(p==2) {
							
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							if(baseStationProbability[p] < 0) {
								baseStationProbability[p] = 0.0;
							}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3))-baseStationProbability[p];//Deadline(cloudlet,p,matrix,slacktime3)

						}
						
						//System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
						
												
					} // end of probability calculation
					
					
					
					// for loop to task allocation
					for(int index = 0;index<3;index++) {
						
						if(checkBS == index) {
							
							if(checkBS == 0 && temProbability[checkBS] > 0.0) {
							
							Deadline(cloudlet, matrix, slacktime1);
							cloudlet.setUserId(broker1.getId());
							batchQueBS1.add(cloudlet);
							
							numTaskAllocatedBS1++;
							allocatedTaskBS1++;

							baseStationProbability[checkBS]+=  0.001;
							
							}
							else if (checkBS == 1 && temProbability[checkBS] > 0.0 ){
							
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 1
									
									Deadline(cloudlet, matrix, slacktime2);
									
									cloudlet.setUserId(broker2.getId());
									batchQueBS2.add(cloudlet);
									
									numTaskAllocatedBS2++;
									allocatedTaskBS2++;
									baseStationProbability[1]+=  0.09;
								}
												
								
							}// end of bs1 check
							else if(checkBS == 2 && temProbability[checkBS] > 0.0 ) {
								
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 2 (or 3rd BS)
									
									Deadline(cloudlet, matrix, slacktime3);
									
									cloudlet.setUserId(broker3.getId());
									batchQueBS3.add(cloudlet);
									
									numTaskAllocatedBS3++;
									allocatedTaskBS3++;
									baseStationProbability[1]+=  0.01;
								}
								
								
								
							}
							else {
								//no allocation
								noTaskNotAllocated++;
							}
							
							
						} // end of main if condition

						
					} // end of index for loop
					
					
					
					
					
								
					
					
//					if(checkBS==0 && temProbability[checkBS]> 0.0) {
//						Deadline(cloudlet, matrix, slacktime1);
//						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
//						cloudlet.setUserId(broker1.getId());
//						batchQueBS1.add(cloudlet);
//						//update etc ett
//						numTaskAllocatedBS1++;
//						allocatedTaskBS1++;
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS1)/convergValue));
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS1)/convergValue));
//						
//						//if(numTaskAllocatedBS1%10==0 && numTaskAllocatedBS1 > 0) {
//						baseStationProbability[checkBS]+=  0.001;
//						//}
//					}
//					else if(checkBS==1 && temProbability[checkBS]> 0.0)
//					{
//						Deadline(cloudlet, matrix, slacktime2);
//						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
//						cloudlet.setUserId(broker2.getId());
//						batchQueBS2.add(cloudlet);
//						numTaskAllocatedBS2++;
//						allocatedTaskBS2++;
//						
//					
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS2)/convergValue));
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS2)/convergValue));
//						//if(numTaskAllocatedBS2%10==0 && numTaskAllocatedBS2>0) {
//						baseStationProbability[checkBS]+=  0.3;
//						//}
//					}
//					else if(checkBS==2 && temProbability[checkBS]> 0.0)
//					{
//						Deadline(cloudlet, matrix, slacktime3);
//						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
//						cloudlet.setUserId(broker3.getId());
//						batchQueBS3.add(cloudlet);
//						numTaskAllocatedBS3++;
//						allocatedTaskBS3++;
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS3)/convergValue));
//						
//						
//						//baseStationProbability[checkBS]=  1/(Math.sqrt((convergValue+numTaskAllocatedBS3)/convergValue));
//						//if(numTaskAllocatedBS3%10==0 && numTaskAllocatedBS3>0) {
//						baseStationProbability[checkBS]+=  0.01;
//						//}
//					}
//					else {
//						//cloudlet.setCloudletStatus(0);
//						noTaskNotAllocated++;
//						
//					}
				
					
					
					
					
				}// end of task type 2
				
				
				
				
				
			}// END OF FOR LOOP
			
			broker1.submitCloudletList(batchQueBS1); 
			broker2.submitCloudletList(batchQueBS2); 
			broker3.submitCloudletList(batchQueBS3);
		
		}// end of else
		
		
		
	}
	
	// Load balancer with task dropping
	
	private static void loadBalancerWithTaskDropping(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3,DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3, ETCmatrix matrix,ETTmatrix ettmatrix, int trial, double dropValue  ) throws Exception {

/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
		
		double loseDeadline = 0;
		
		double slacktime1 = 1.45;
		double slacktime2 = 1.52;
		double slacktime3 = 1.79;
	
		noTaskNotAllocated = 0;			
		taskMissDBS1 = 0;
		taskMissDBS2 = 0;
		
		
		double temProbability[] = new double[3];
		for(int y=0;y<3;y++) {
			temProbability[y] = 0.0;
		}
		
		
		double baseStationProbability[] = new double[3];
		for(int b=0;b<3;b++) {
			baseStationProbability[b] = 0.0;
		}
	
		double check;
		int checkBS;

		double convMu;
		double convSigma;
		double convSigma1;
		double convSigma2;
		
		
		//int convergValue = 385;
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker1.getId());
				
				System.out.println("broker 1 id : "+ broker1.getId());
				batchQueBS1.add(cloudlet);
				deadLineFirst(cloudlet, slacktime1,vmMips1);
			}
			
			broker1.submitCloudletList(batchQueBS1); 
	
		}
		else if(trial == 1) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker2.getId());
				System.out.println("broker 2 id : "+ broker2.getId());
				batchQueBS2.add(cloudlet);
				deadLineFirst(cloudlet, slacktime2,vmMips2);
			}
			broker2.submitCloudletList(batchQueBS2);
		}
		else if(trial == 2) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker3.getId());
				System.out.println("broker 2 id : "+ broker3.getId());
				batchQueBS3.add(cloudlet);
				deadLineFirst(cloudlet, slacktime3,vmMips3);
			}
			broker3.submitCloudletList(batchQueBS3);
		}
		else {
			
			for(Cloudlet cloudlet: arrivingCloudlets)
			{
				
				if(cloudlet.getTaskType() == 1) {
                    
					System.out.println("%%%%%%% Task type 1 %%%%%%%%%%%");
					check = -1.00;
					checkBS = -1;
					
					for(int p=0;p<3;p++) {
						if (p==0) {
						
							//if(baseStationProbability[p] < 0) {
							//	baseStationProbability[p] = 0.0;
							//}
							
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1))-baseStationProbability[p];
							
											
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							//if(baseStationProbability[p] < 0) {
							//	baseStationProbability[p] = 0.0;
							//}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2))-baseStationProbability[p];
							//baseStationProbability[p] = baseStationProbability[p]+temProbability[p];
						
													}
						else if(p==2) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							//if(baseStationProbability[p] < 0) {
							//	baseStationProbability[p] = 0.0;
							//}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3))-baseStationProbability[p];
						}
						
						//System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
		
					
					}// end of probability calculation
					
					
					// for loop to task allocation
					for(int index = 0;index<3;index++) {
						
						if(checkBS == index) {
							
							if(checkBS == 0 && temProbability[checkBS] > dropValue) { // receiving base station
							
							Deadline(cloudlet, matrix, slacktime1);
							cloudlet.setUserId(broker1.getId());
							batchQueBS1.add(cloudlet);
							
							numTaskAllocatedBS1++;
							allocatedTaskBS1++;

							baseStationProbability[checkBS]+=  0.001; // base station 0
							
							}
							else if (checkBS == 1 && temProbability[checkBS] > dropValue ){ // base station 1
							
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 1
									
									Deadline(cloudlet, matrix, slacktime2);
									
									cloudlet.setUserId(broker2.getId());
									batchQueBS2.add(cloudlet);
									
									numTaskAllocatedBS2++;
									allocatedTaskBS2++;
									baseStationProbability[1]+=  0.09;
								}
												
								
							}// end of bs1 check
							else if(checkBS == 2 && temProbability[checkBS] > dropValue ) {
								
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 2 (or 3rd BS)
									
									Deadline(cloudlet, matrix, slacktime3);
									
									cloudlet.setUserId(broker3.getId());
									batchQueBS3.add(cloudlet);
									
									numTaskAllocatedBS3++;
									allocatedTaskBS3++;
									baseStationProbability[2]+=  0.01;
								}
								
							
							}
							else {
								//no allocation
								noTaskNotAllocated++;
							}
							
							
						} // end of main if condition

						
					} // end of index for loop
					
			
			}// end of task type 1 checking 
			else if(cloudlet.getTaskType() == 2) {
				   System.out.println("%%%%%%% Task type 2 %%%%%%%%%%%");
				   
					check = -1.00;
					checkBS = -1;
					
	
					for(int p = 0; p < 3; p++) { // calculating the probability and store the highest probability
						if (p==0) {
							
							if(baseStationProbability[p] < 0) {
								baseStationProbability[p] = 0.0;
							}
							
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1))-baseStationProbability[p];
							
						
							
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							if(baseStationProbability[p] < 0) {
								baseStationProbability[p] = 0.0;
							}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2))-baseStationProbability[p];//Deadline(cloudlet,p,matrix,slacktime2)

						
						}
						else if(p==2) {
							
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							if(baseStationProbability[p] < 0) {
								baseStationProbability[p] = 0.0;
							}
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3))-baseStationProbability[p];//Deadline(cloudlet,p,matrix,slacktime3)

						}
						
						//System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
						
												
					} // end of probability calculation
					
					
					
					// for loop to task allocation
					for(int index = 0;index<3;index++) {
						
						if(checkBS == index) {
							
							if(checkBS == 0 && temProbability[checkBS] > dropValue) {
							
							Deadline(cloudlet, matrix, slacktime1);
							cloudlet.setUserId(broker1.getId());
							batchQueBS1.add(cloudlet);
							
							numTaskAllocatedBS1++;
							allocatedTaskBS1++;

							baseStationProbability[checkBS]+=  0.001;
							
							}
							else if (checkBS == 1 && temProbability[checkBS] > dropValue ){
							
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 1
									
									Deadline(cloudlet, matrix, slacktime2);
									
									cloudlet.setUserId(broker2.getId());
									batchQueBS2.add(cloudlet);
									
									numTaskAllocatedBS2++;
									allocatedTaskBS2++;
									baseStationProbability[1]+=  0.09;
								}
												
								
							}// end of bs1 check
							else if(checkBS == 2 && temProbability[checkBS] > dropValue ) {
								
								convSigma1 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 1), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 1), 2)));
								convSigma2 = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), 2), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), 2), 2)));	
								
								if(temProbability[1]==temProbability[2]) { // if there is a tie 
									
									if(convSigma1 < convSigma2) {
										//allocate task to bs 1
										Deadline(cloudlet, matrix, slacktime2);
										
										cloudlet.setUserId(broker2.getId());
										batchQueBS2.add(cloudlet);
										
										numTaskAllocatedBS2++;
										allocatedTaskBS2++;
										baseStationProbability[1]+=  0.09;
										
									}else {
										//allocate task to bs 2
										Deadline(cloudlet, matrix, slacktime3);
										cloudlet.setUserId(broker3.getId());
										batchQueBS3.add(cloudlet);
										
										numTaskAllocatedBS3++;
										allocatedTaskBS3++;
										baseStationProbability[2]+=  0.01;
									}
								}
								else { // there is no tie and Base Station is 2 (or 3rd BS)
									
									Deadline(cloudlet, matrix, slacktime3);
									
									cloudlet.setUserId(broker3.getId());
									batchQueBS3.add(cloudlet);
									
									numTaskAllocatedBS3++;
									allocatedTaskBS3++;
									baseStationProbability[1]+=  0.01;
								}
								
								
								
							}
							else {
								//no allocation
								noTaskNotAllocated++;
							}
							
							
						} // end of main if condition

						
					} // end of index for loop
					
				
				}// end of task type 2
				
				
				
				
				
			}// END OF FOR LOOP
			
			broker1.submitCloudletList(batchQueBS1); 
			broker2.submitCloudletList(batchQueBS2); 
			broker3.submitCloudletList(batchQueBS3);
		
		}// end of else
	
		
		
	}
	
	private static void loadBalancerMECT(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3, ETCmatrix matrix,ETTmatrix ettmatrix, int trial) {
	
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
		
		//double slacktime1 = 0.25;
		//double slacktime2 = 0.12;
		//double slacktime3 = 0.19;
		
		double loseDeadline=0;
		
		double slacktime1 = 1.45;
		double slacktime2 = 1.52;
		double slacktime3 = 1.79;
		
		noTaskNotAllocated = 0;			
		taskMissDBS1 = 0;
		taskMissDBS2 = 0;
		
		
		double tempCompletionTime[] = new double[3];
		for(int h=0;h<3;h++) {
			tempCompletionTime[h]=0.0;
		}
		
		double baseStationProbability[] = new double[3];
		for(int b=0;b<3;b++) {
			baseStationProbability[b] = 0.0;
		}
		

		double check;
		int checkBS;
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker1.getId());
				batchQueBS1.add(cloudlet);
				deadLineFirst(cloudlet, slacktime1,vmMips1);
			}
			
			broker1.submitCloudletList(batchQueBS1); 
	
		}
		else if(trial == 1) {
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker2.getId());
				batchQueBS2.add(cloudlet);
				deadLineFirst(cloudlet, slacktime2,vmMips2);
			}
			broker2.submitCloudletList(batchQueBS2);
		}
		else if(trial == 2) {
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker3.getId());
				batchQueBS3.add(cloudlet);
				deadLineFirst(cloudlet, slacktime3,vmMips3);
			}
			broker3.submitCloudletList(batchQueBS3);
		}
		else {
			
			for(Cloudlet cloudlet: arrivingCloudlets)
			{
				
				if(cloudlet.getTaskType() == 1) {

					check = 999999.9;
					checkBS = -1;
					
					for(int p=0;p<3;p++) {
					
						if(p==0) {
						
							tempCompletionTime[p] = matrix.getMu(cloudlet.getTaskType(), p)-baseStationProbability[p];;
						}			
						else if(p==1) {
							tempCompletionTime[p] = (matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p))-baseStationProbability[p];
						}
						else if(p==2) {
							tempCompletionTime[p] = (matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p))-baseStationProbability[p];
						}
						
						
						if(tempCompletionTime[p] < check) {
							
							check = tempCompletionTime[p];
							checkBS = p;
						}
					}
					
					if(checkBS == 0) {
						Deadline(cloudlet, matrix, slacktime1);
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
						numTaskAllocatedBS1++;
						baseStationProbability[checkBS]+=  0.001;
						
					}
					else if(checkBS==1)
					{
						Deadline(cloudlet, matrix, slacktime2);
						cloudlet.setUserId(broker2.getId());
						batchQueBS2.add(cloudlet);
						numTaskAllocatedBS2++;
						baseStationProbability[checkBS]+=  0.09;
						
					}
					else if(checkBS==2)
					{
						Deadline(cloudlet, matrix, slacktime3);
						cloudlet.setUserId(broker3.getId());
						batchQueBS3.add(cloudlet);
						numTaskAllocatedBS3++;
						baseStationProbability[checkBS]+=  0.01;
						
						
					}
					else {
						noTaskNotAllocated++;
					}
	
				} // END of task type 1
				
				else if(cloudlet.getTaskType() == 2) {
					
					check = 999999.9;
					checkBS = -1;

					for(int p = 0; p < 3; p++) { // calculating min completion time
						
						if(p==0) {
						tempCompletionTime[p] = matrix.getMu(cloudlet.getTaskType(), p)-baseStationProbability[p];
						
						}
						else if(p==1) {
						tempCompletionTime[p] = (matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p))-baseStationProbability[p];
							
						}
						else if(p==2)
						{
							tempCompletionTime[p] = (matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p))-baseStationProbability[p];
						}
						
						
						if(tempCompletionTime[p] < check) {
							
							check = tempCompletionTime[p];
							checkBS = p;
						}
					} // end of probability calculation
					
					if(checkBS==0) {
						Deadline(cloudlet, matrix, slacktime1);
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
						//update etc ett
						
						numTaskAllocatedBS1++;
						baseStationProbability[checkBS]+=  0.001;
						
					}
					else if(checkBS==1)
					{
						Deadline(cloudlet, matrix, slacktime2);
						cloudlet.setUserId(broker2.getId());
						batchQueBS2.add(cloudlet);
						numTaskAllocatedBS2++;
						baseStationProbability[checkBS]+=  0.09;
						
						
					}
					else if(checkBS==2)
					{
						Deadline(cloudlet, matrix, slacktime3);
						cloudlet.setUserId(broker3.getId());
						batchQueBS3.add(cloudlet);
						numTaskAllocatedBS3++;
						baseStationProbability[checkBS]+=  0.01;
						
						
					}
					else {
						noTaskNotAllocated++;
						
					}
				
				}
			}// END OF FOR LOOP
			
			broker1.submitCloudletList(batchQueBS1); 
			broker2.submitCloudletList(batchQueBS2); 
			broker3.submitCloudletList(batchQueBS3);
		
		}// end of else
		
		
	}
	
	
     /**
	 * Creates main() to run the example
	 * @throws IOException 
	 */
	
	
	public static void main(String[] args) throws IOException {
		Log.printLine("Starting Simulation for V2I task processing...");
		int numTasksAdmitted=0;
		int number = 0;
		int arrivingTasks = 0;
		int lbOption = -1;
		long userSeed=0;
		int initialWorkload=50;
		
		//boolean lbOnOff = false;
		String SchPolicy=null;
		taskDstrList = new ArrayList<Cloudlet>();
		
		
		FileWriter fw = new FileWriter("ResultV2I.txt",true);
		PrintWriter printWriter = new PrintWriter(fw);
		printWriter.println();
		printWriter.println("Starting Simulation for V2I task processing...");
		printWriter.println();
		//printWriter.println("100 trials, 100 clouldets and Using No load Balancer");
		
		try{
		
			File file = new File("input.txt"); 									/* Read from the input file. */
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
		
//		 arrList1 = new double[5826];
//		
//	     File file1 = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival2.dat");
//	        
//	     BufferedReader br1 = new BufferedReader(new FileReader(file1));
//	     try {
//	            int index = 0;
//	            String line = null;
//	            while ((line=br1.readLine())!=null) {
//	               
//	                String[] lineArray = line.split(",");
//              
//	                arrList1[index]= Double.parseDouble(lineArray[2]);
//
//	                index++;
//	            							} 
//	        }catch (FileNotFoundException e) {
//	            e.printStackTrace();
//	     }
    
//	     
//		 arrList2 = new double[6848];
//			
//	     File file2 = new File("/home/c00303945/Downloads/cloudsim-3.0.3/arrival3.dat");
//	        
//	     BufferedReader br2 = new BufferedReader(new FileReader(file2));
//	     try {
//	            int index = 0;
//	            String line = null;
//	            while ((line=br2.readLine())!=null) {
//	               
//	                String[] lineArray = line.split(",");
//               
//	                arrList2[index]= Double.parseDouble(lineArray[2]);
//
//	                index++;
//	            							} 
//	        }catch (FileNotFoundException e) {
//	            e.printStackTrace();
//	     } 
//		
	
		/* Trial loop */
	       
	   // long startTime = System.currentTimeMillis();   
		
	    for(int i = 0;i <number;i++) {
			
			try {
			printWriter.println("Trial No : "+ i);
			//allTaskNotAllocated = ;
			
			/* First step: initialize the CloudSim package. It should be called before creating any entities.*/
			
			int num_user =1;   												/* Number of grid users.*/
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  									/* Mean trace events.*/
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");			/* Added by Razin.*/
			CloudSim.init(num_user, calendar, trace_flag);
			
			/* Second step: create 3 Datacenters/Base Station.*/ 
			
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("BaseStation_0",1,0,60,54000000);	/* Base Station x with coordinates 0 and the range of 60 meters. */
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("BaseStation_1",1,5,65,54000000);	/* Base Station x  with coordinates 5 and the range of 65 meters. */
			@SuppressWarnings("unused")
			Datacenter datacenter2 = createDatacenter("BaseStation_2",1,-2.5,52,50000000); /* Base Station x  with coordinates -2.5 and the range of 50 meters. */
			//////End Of Base Station Creation//////
			
			
			/* Third step: Create Brokers.*/
			DatacenterBroker broker1 = createBroker("broker1");
			int vmMips1 = 1500;
			vmlist1 = createVM_N(broker1.getId(), 5,vmMips1, 1);
			broker1.submitVmList(vmlist1);
			broker1.setSchedulerPolicy(SchPolicy);
			
			////////////////////////////////////////////////////
			
			DatacenterBroker broker2 = createBroker("broker2");
			int vmMips2 = 2000;
			vmlist2 = createVM_N(broker2.getId(), 5,vmMips2, 1001);
			broker2.submitVmList(vmlist2);
			broker2.setSchedulerPolicy(SchPolicy);
			
			//////////////////////////////////////////////////////
			
			DatacenterBroker broker3 = createBroker("broker3");
			int vmMips3 = 2500;
			vmlist3 = createVM_N(broker3.getId(), 5,vmMips3, 2001);
			broker3.submitVmList(vmlist3);
			broker3.setSchedulerPolicy(SchPolicy);
			
			/* End of broker creation - Razin.*/
		
			if(i>2) {
				
				userSeed = userSeed +20000;
				
			}
			
			numTasksAdmitted = arrivingTasks;
			cloudletList3 = createCloudlet(broker1.getId(),numTasksAdmitted,100,2000,550000,userSeed); // Testing workload.cloudlet ID starts from 3000
			
			
//			double at = 0.0;
//			for(Cloudlet cloudlet:cloudletList3) {
//				if(cloudlet.getArrivalTime() > at) {
//					at = cloudlet.getArrivalTime();
//				}
//				
//			}
//			printWriter.println();
//			printWriter.println ("Latest arrivaltime : "+at);
//			printWriter.println();
			
//			overSubsList1 = createCloudlet(broker1.getId(),1000,100,2000,7000,userSeed);
//			for(int h=0;h<1000;h++) {
//				
//				if (arrList1[h] < at) {
//					overSubsList1.get(h).setArrivalTime(arrList1[h]);
//				}
//				
//			}
			
			// Generating three workload for 3 Base Station
			initialWorkload = 1000;
			
     		cloudletList4 = createInitialWorkLoad(broker1.getId(),initialWorkload,100,2000,1000,1850);
     		cloudletList5 = createInitialWorkLoad(broker2.getId(),initialWorkload,100,2000,30000,15000);
     		cloudletList6 = createInitialWorkLoad(broker3.getId(),initialWorkload,100,2000,60000,3986);
     		//Workload END
     		
//     		
//     		for(Cloudlet cloudlet:cloudletList6) {
//     			
//     			printWriter.println(" broker id :::::" + broker3.getId());
//     			printWriter.println(" user id :::::" + cloudlet.getUserId());
//     			
//     		}
//		
     		numTaskAllocatedBS1 = 0;
    		numTaskAllocatedBS2 = 0;
    		numTaskAllocatedBS3 = 0;
    		
    		allocatedTaskBS1 = 0;
    		allocatedTaskBS2 = 0;
    		allocatedTaskBS3 = 0;
    		
    		if (lbOption == 1) { // load balancer on without task dropping
     			if(i == 0) {
     				//broker1.submitCloudletList(cloudletList4); 
     				
     				loadBalancer(cloudletList4, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i == 1) {
     				loadBalancer(cloudletList5, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     				//broker2.submitCloudletList(cloudletList5);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i == 2) {
     				loadBalancer(cloudletList6, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     				//broker3.submitCloudletList(cloudletList6);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else {
     				/*
     				for(Cloudlet cloudlet:cloudletList4) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				for(Cloudlet cloudlet:cloudletList5) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				for(Cloudlet cloudlet:cloudletList6) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				*/
     				broker1.submitCloudletList(cloudletList4); 
     				broker2.submitCloudletList(cloudletList5); 
     				broker3.submitCloudletList(cloudletList6);
     				loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     			}
     			
			}
     		else if(lbOption==2) { // load balancer with task dropping
     			if(i==0) {
     				loadBalancerWithTaskDropping(cloudletList4, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,0.7);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i==1) {
     				loadBalancerWithTaskDropping(cloudletList5, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,0.7);
     				
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i==2) {
     				loadBalancerWithTaskDropping(cloudletList6, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,0.7);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else {
     				/*
     				for(Cloudlet cloudlet:cloudletList4) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				for(Cloudlet cloudlet:cloudletList5) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				for(Cloudlet cloudlet:cloudletList6) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				*/
     				broker1.submitCloudletList(cloudletList4); 
     				broker2.submitCloudletList(cloudletList5); 
     				broker3.submitCloudletList(cloudletList6);
     				loadBalancerWithTaskDropping(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,matrix,ettMatrix,i,0.7);
     			}
     			
     			
     		}
     		else if(lbOption==4) { // load balancer MECT
     			if(i==0) {
     				loadBalancerMECT(cloudletList4, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i==1) {
     				loadBalancerMECT(cloudletList5, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
     				
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i==2) {
     				loadBalancerMECT(cloudletList6, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else {
     				
     				for(Cloudlet cloudlet:cloudletList4) {
     					Deadline(cloudlet,matrix,1.45);
     				}
     				for(Cloudlet cloudlet:cloudletList5) {
     					Deadline(cloudlet,matrix,1.52);
     				}
     				for(Cloudlet cloudlet:cloudletList6) {
     					Deadline(cloudlet,matrix,1.79);
     				}
     								
     				broker1.submitCloudletList(cloudletList4); 
     				broker2.submitCloudletList(cloudletList5); 
     				broker3.submitCloudletList(cloudletList6);
   				
     				loadBalancerMECT(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
     				
     			}
     			
     			
     		}
     		else {
				if(i==0) {
     				loadBalancer(cloudletList4, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i==1) {
     				loadBalancer(cloudletList5, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     				
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else if(i==2) {
     				loadBalancer(cloudletList6, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3,datacenter0,datacenter1,datacenter2, matrix,ettMatrix,i);
     				//loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,startTime);
     			}
     			else {
     				
     				for(Cloudlet cloudlet:cloudletList4) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				for(Cloudlet cloudlet:cloudletList5) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				for(Cloudlet cloudlet:cloudletList6) {
     					Deadline(cloudlet,matrix,0.12);
     				}
     				
     				
     				broker1.submitCloudletList(cloudletList4); 
     				broker2.submitCloudletList(cloudletList5); 
     				broker3.submitCloudletList(cloudletList6);
     				
     				double nLD = 10;
     				
     				for(Cloudlet cloudlet:cloudletList3) {
     					Deadline(cloudlet,matrix,1.45);
     				}
     				broker1.submitCloudletList(cloudletList3);
     			}
     			

			}
     		
		   /* Fifth step: Start the simulation. */
			
			CloudSim.startSimulation();
			
			
			/* Final step: Print results when simulation is over. */
			
			List<Cloudlet> newList = broker1.getCloudletReceivedList();
			newList.addAll(broker2.getCloudletReceivedList());
			newList.addAll(broker3.getCloudletReceivedList());
			
//			double at = 0.0;
//			for(Cloudlet cloudlet:cloudletList3) {
//				if(cloudlet.getArrivalTime() > at) {
//					at = cloudlet.getArrivalTime();
//				}
//				
//			}
//			printWriter.println();
//			printWriter.println ("Latest arrivaltime : "+at);
//			printWriter.println();
			
			taskDstrList.addAll(newList);
			if(i==0) {
				
				getDistribution(cloudletList4);
				getETTDistribution(cloudletList4, datacenter0, datacenter1, datacenter2);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
				
			}
			else if(i==1) {
				getDistribution(cloudletList5);
				getETTDistribution(cloudletList5, datacenter0, datacenter1, datacenter2);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
			}
			else if(i==2) {
				getDistribution(cloudletList6);
				getETTDistribution(cloudletList6, datacenter0, datacenter1, datacenter2);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
			}
			else {
				getDistribution(cloudletList3);
				getETTDistribution(cloudletList3, datacenter0, datacenter1, datacenter2);
				
				ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
				matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);
			}
			
			
			// pasuing the simulation for 1 seconds between each trial 
//			long stopTime = System.currentTimeMillis();
//			
//			Random random = new Random();
//			
//			while((stopTime - startTime) < 1000) {
//				
//				stopTime = System.currentTimeMillis();
//			}
			//End of pausing
			
			CloudSim.stopSimulation();

            //System.out.println("^^^^^^^^ list of cloudlets^^^^^^^^^^^ "+taskDstrList.size()); 
			 printCloudletList(newList,printWriter,numTasksAdmitted);
		
			
			allTaskNotAllocated.add(noTaskNotAllocated);
			printWriter.println();
			
			//if (i==102) {
			//System.out.println(" All counter array ::"+ allCounters.toString());
			printWriter.println(" All counter array ::"+ allCounters.toString());
			printWriter.println();
			printWriter.println(" allTaskNotAllocated array ::"+ allTaskNotAllocated.toString());
			//}
			
			
			//printWriter.println();
			
			//printWriter.println();
			System.out.println(" Task not allocated Array ::"+ allTaskNotAllocated.toString());
			//printWriter.println(" Task not allocated Array ::"+ allTaskNotAllocated.toString());
			//printWriter.println();
			
			
			//HashMap<String, NormalDistribution> newDistr = getDistribution(taskDstrList);
			//matrix = new ETCmatrix(taskTypeNum, dataCenterNum, newDistr);
			
			//matrix.printMatrix();
			System.out.println("$$$$$$$ Type 1 , Base Station 0 : " + matrix.getSigma(1,0) + " " + matrix.getMu(1, 0)); 
			System.out.println("$$$$$$$ Type 1 , Base Station 1 : " + matrix.getSigma(1,1) + " " + matrix.getMu(1, 1));
			System.out.println("$$$$$$$ Type 1 , Base Station 2 : " + matrix.getSigma(1,2) + " " + matrix.getMu(1, 2));
			
			System.out.println("$$$$$$$ ETT matrix Type 1 , Base Station 0 : " + ettMatrix.getSigma(1,0) + " " + ettMatrix.getMu(1, 0));
			System.out.println("$$$$$$$ ETT matrix Type 1 , Base Station 1 : " + ettMatrix.getSigma(1,1) + " " + ettMatrix.getMu(1, 1));
			System.out.println("$$$$$$$ ETT matrix Type 1 , Base Station 2 : " + ettMatrix.getSigma(1,2) + " " + ettMatrix.getMu(1, 2));
			
			printWriter.println();
			printWriter.println("$$$$$$$ Type 1 , Base Station 0 : " + matrix.getSigma(1,0) + " " + matrix.getMu(1, 0)); 
			printWriter.println("$$$$$$$ Type 1 , Base Station 1 : " + matrix.getSigma(1,1) + " " + matrix.getMu(1, 1));
			printWriter.println("$$$$$$$ Type 1 , Base Station 2 : " + matrix.getSigma(1,2) + " " + matrix.getMu(1, 2));
						
			
			System.out.println("$$$$$$$ Type 2 , Base Station 0 : " + matrix.getSigma(2,0) + " " + matrix.getMu(2, 0));
			System.out.println("$$$$$$$ Type 2 , Base Station 1 : " + matrix.getSigma(2,1) + " " + matrix.getMu(2, 1));
			System.out.println("$$$$$$$ Type 2 , Base Station 2 : " + matrix.getSigma(2,2) + " " + matrix.getMu(2, 2));
			
			System.out.println();
			
			System.out.println("$$$$$$$ ETT matrix Type 2 , Base Station 0 : " + ettMatrix.getSigma(2,0) + " " + ettMatrix.getMu(2, 0));
			System.out.println("$$$$$$$ ETT matrix Type 2 , Base Station 1 : " + ettMatrix.getSigma(2,1) + " " + ettMatrix.getMu(2, 1));
			System.out.println("$$$$$$$ ETT matrix Type 2 , Base Station 2 : " + ettMatrix.getSigma(2,2) + " " + ettMatrix.getMu(2, 2));
			
			
			printWriter.println("$$$$$$$ Type 2 , Base Station 0 : " + matrix.getSigma(2,0) + " " + matrix.getMu(2, 0)); 
			printWriter.println("$$$$$$$ Type 2 , Base Station 1 : " + matrix.getSigma(2,1) + " " + matrix.getMu(2, 1));
			printWriter.println("$$$$$$$ Type 2 , Base Station 2 : " + matrix.getSigma(2,2) + " " + matrix.getMu(2, 2));
			
			
			double deadLineMissRate = noTaskNotAllocated / (double)numTasksAdmitted;
			System.out.println("No of task not allocated : "+ noTaskNotAllocated +" Allocation miss rate : "+ deadLineMissRate);
			printWriter.println("No of task not allocated : "+ noTaskNotAllocated +" Allocation miss rate : "+ deadLineMissRate);
			
			
			System.out.println(" No of task not allocated ::::@@@#########  "+noTaskNotAllocated);
			System.out.println(" Task Allocated to BS0 :" + numTaskAllocatedBS1 + " Task Allocated to BS1 :" + numTaskAllocatedBS2 +" Task Allocated to BS2 :" +numTaskAllocatedBS3);
			
			printWriter.println();
			printWriter.println(" No of task not allocated ::::@@@#########  "+noTaskNotAllocated);
			
			System.out.println("No of task missed deadline in Base Station 1 : " + taskMissDBS1);
			printWriter.println("No of task missed deadline in Base Station 1 : " + taskMissDBS1);
			
			System.out.println("No of task missed deadline in Base Station 2 : " + taskMissDBS2);
			printWriter.println("No of task missed deadline in Base Station 2 : " + taskMissDBS2);
			
			printWriter.println(" Task Allocated to BS0 :" + numTaskAllocatedBS1 + " Task Allocated to BS1 :" + numTaskAllocatedBS2 +" Task Allocated to BS2 :" +numTaskAllocatedBS3);
			Log.printLine("V2I task processing finished!");

			printWriter.println();
			printWriter.println("End of trail : "+ i);
		
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
		
		for(int x=0;x<sizeOfArray;x++)
		{
			sum = sum + allCounters.get(x);
			
			if(x>3) {
				if(allCounters.get(x)>checkMax) {
					
					checkMax = allCounters.get(x);
				}
				
				if(allCounters.get(x)<checkMin) {
					checkMin = allCounters.get(x);
				}
			}
			
		}
		
		result = (sum/(sizeOfArray-3));
		
		
		double sd = 0;
		for (int i = 3; i <sizeOfArray; i++)
		{
			
		    sd += Math.pow((allCounters.get(i) - result), 2) / (sizeOfArray-3);
		}
		double standardDeviation = Math.sqrt(sd);
		
		
		
		String str = null;
		if(lbOption == 1) {
			str = "with MR load balancer ("+SchPolicy+")";
		}
		else if(lbOption==2) {
			str = "with load balancer task dropping probability threshold ("+SchPolicy+")";
		}
		else if(lbOption==4) {
			str = "with MECT load balancer ("+SchPolicy+")";
		}
		else {str = "with no load balancer ("+SchPolicy+")";};
		
		FileWriter flwrt = new FileWriter("/home/c00303945/Desktop/Results_6th_April/FCFS_increased_Arrival_Task.txt",true);
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
	
	

	private static Datacenter createDatacenter(String name, int hostNumber, double x,double range,long linkBW){

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
		//peList1.add(new Pe(5, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(6, new PeProvisionerSimple(mips)));
		//peList1.add(new Pe(7, new PeProvisionerSimple(mips)));
		//8 cores
		
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
	
private static void getETTDistribution(List<Cloudlet> list, Datacenter dc0, Datacenter dc1, Datacenter dc2) {
		
		Cloudlet cloudlet;
		int size = list.size();
		//long timeMillis = System.currentTimeMillis(); // current time in miliseconds
        //long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);// current time in seconds
		
			
		for(int i = 0; i < size; i++) {
			double transferTime = 0.00;
			cloudlet = list.get(i);
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				
				String key = cloudlet.getTaskType() + "." + (cloudlet.getResourceId()-2);
				
				if((cloudlet.getResourceId()-2)==0)
				{
					transferTime = (double)cloudlet.getCloudletFileSize()/dc0.getBandWidth();
				}
				else if((cloudlet.getResourceId()-2)==1) {
				
					transferTime = (double)cloudlet.getCloudletFileSize()/dc1.getBandWidth();
				}
				else if((cloudlet.getResourceId()-2)==2) {
					
					transferTime = (double)cloudlet.getCloudletFileSize()/dc2.getBandWidth();
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
		
		//FileWriter fileWriter = new FileWriter("ResultV2I.txt",true);
		//PrintWriter printWriter = new PrintWriter(fileWriter);	
		
		//int allCounters[] = new int[100];
		
		
		
		int counter = 0;
		String indent = "    ";
		Log.printLine();
		pw.println();//file write
		Log.printLine("========== OUTPUT ==========");
		pw.println("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent + indent+ "VM ID" + indent + indent+"  "+ "Execution Time"+indent+indent +"Task Length(MI)"+indent+"Task Type"+indent+indent+ "Start Time" + indent + "Finish Time"+indent+indent+indent+"Deadline"+indent+indent+"Completion Time"+indent+indent+"DeadlineMet"+indent+indent+"Arrival Time");
        
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
				
				if(cloudlet.getCloudletId() >=550000 && cloudlet.getCloudletId()<= (550000+numOfTaskAdmitted))
				{
					if(cloudlet.getDeadline()<cloudlet.getFinishTime()) {
						str = "False";
						counter++;
					}
					else {
						str ="True";
					}
				
					
				}
				
				Log.printLine( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent +indent+indent+indent +cloudlet.getCloudletLength()+ indent + indent +indent+indent+ cloudlet.getTaskType()+indent+indent+indent+ indent+dft.format(cloudlet.getExecStartTime())+ indent + indent+indent + dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+dft.format(cloudlet.getDeadline())+indent+indent+indent+indent+dft.format(cloudlet.getFinishTime() - cloudlet.getArrivalTime())+indent+indent+indent+indent+str+indent+indent+indent+cloudlet.getArrivalTime());
				
				
				pw.println( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent+indent+indent+indent + cloudlet.getCloudletLength()+ indent + indent +indent+indent+cloudlet.getTaskType()+indent +indent+indent+indent +dft.format(cloudlet.getExecStartTime())+ indent + indent + indent+dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+dft.format(cloudlet.getDeadline())+indent+indent+indent+indent+dft.format(cloudlet.getFinishTime() - cloudlet.getArrivalTime())+indent+indent+indent+indent+str+indent+indent+indent+cloudlet.getArrivalTime());
				
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
