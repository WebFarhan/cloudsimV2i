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

	
	private static List<Cloudlet> cloudletList1,cloudletList2,cloudletList3,cloudletList4,taskDstrList;
	private static int SEED = 120;
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
			length = randomMIGenerator(START, END,rObj);
			
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
			seed--;
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
		
		//long timeMillis = System.currentTimeMillis();
       // long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis);
		
		double deadline = (b.getMu(a.getTaskType(), 0)+b.getMu(a.getTaskType(),1)+b.getMu(a.getTaskType(),2))/3 + slack;
		
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

	/*private static double zScoreCalculation(double mu, double sigma, double value) {
		
		double result = 0;
		
		result = (value - mu)/sigma;
		
		return round(result,2);
	}*/
	
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
	private static void loadBalancer(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3, ETCmatrix matrix,ETTmatrix ettmatrix, int trial  ) throws Exception {
		
		/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
					
		double slacktime1 = 0.25;
		double slacktime2 = 0.12;
		double slacktime3 = 0.19;
		noTaskNotAllocated = 0;			
		taskMissDBS1 = 0;
		taskMissDBS2 = 0;
		
		
		double temProbability[] = new double[3];
		double check;
		int checkBS;
		
		double convMu;
		double convSigma;
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker2.getId());
				batchQueBS2.add(cloudlet);
				deadLineFirst(cloudlet, slacktime2,vmMips2);
			}
			
			broker2.submitCloudletList(batchQueBS2); 
						
	
		}
		else if(trial == 1) {
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker1.getId());
				batchQueBS1.add(cloudlet);
				deadLineFirst(cloudlet, slacktime1,vmMips1);
			}
			broker1.submitCloudletList(batchQueBS1);
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
			
			//System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ In ETC matrix !!! @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
			
			for(Cloudlet cloudlet: arrivingCloudlets)
			{
				
				if(cloudlet.getTaskType() == 1) {

					check = -1.00;
					checkBS = -1;
					
					for(int p=0;p<3;p++) {
						if (p==0) {
						
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1));
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2));
						}
						else if(p==2) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3));
				
						}
						
						System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
					}
					
					if(checkBS == 0 && temProbability[checkBS]!= 0.0) {
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
						numTaskAllocatedBS1++;
					}
					else if(checkBS==1 && temProbability[checkBS]!= 0.0)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker2.getId());
						batchQueBS2.add(cloudlet);
						numTaskAllocatedBS2++;
						
					}
					else if(checkBS==2 && temProbability[checkBS]!= 0.0)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker3.getId());
						batchQueBS3.add(cloudlet);
						numTaskAllocatedBS3++;
						
					}
					else {
						//cloudlet.setCloudletStatus(0);
						noTaskNotAllocated++;
					}
	
				}
				
				else if(cloudlet.getTaskType() == 2) {
					
					check = -1.00;
					checkBS = -1;

					for(int p = 0; p < 3; p++) { // calculating the probability and store the highest probability
						if (p==0) {
							
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1));
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2));//Deadline(cloudlet,p,matrix,slacktime2)
						}
						else if(p==2) {
							
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3));//Deadline(cloudlet,p,matrix,slacktime3)
				
						}
						
						System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
					} // end of probability calculation
					
					if(checkBS==0 && temProbability[checkBS]!= 0.0) {
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
						//update etc ett
						
						numTaskAllocatedBS1++;
					}
					else if(checkBS==1 && temProbability[checkBS]!= 0.0)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker2.getId());
						batchQueBS2.add(cloudlet);
						numTaskAllocatedBS2++;
						
					}
					else if(checkBS==2 && temProbability[checkBS]!= 0.0)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker3.getId());
						batchQueBS3.add(cloudlet);
						numTaskAllocatedBS3++;
						
					}
					else {
						cloudlet.setCloudletStatus(0);
						noTaskNotAllocated++;
						
					}
				
				}
			}// END OF FOR LOOP
			
			broker1.submitCloudletList(batchQueBS1); 
			broker2.submitCloudletList(batchQueBS2); 
			broker3.submitCloudletList(batchQueBS3);
		
		}// end of else
		
		
		
	}
	
	// Load balancer with task dropping
	
	private static void loadBalancerWithTaskDropping(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3, ETCmatrix matrix,ETTmatrix ettmatrix, int trial, double dropValue  ) throws Exception {

		/* Create three batch queues for respective Base Stations. */
		
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
					
		double slacktime1 = 0.25;
		double slacktime2 = 0.12;
		double slacktime3 = 0.19;
		noTaskNotAllocated = 0;			
		taskMissDBS1 = 0;
		taskMissDBS2 = 0;
		
		
		double temProbability[] = new double[3];
		double check;
		int checkBS;
		
		double convMu;
		double convSigma;
		
		if(trial == 0)
		{		
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker2.getId());
				batchQueBS2.add(cloudlet);
				deadLineFirst(cloudlet, slacktime2,vmMips2);
			}
			
			broker2.submitCloudletList(batchQueBS2); 
						
	
		}
		else if(trial == 1) {
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker1.getId());
				batchQueBS1.add(cloudlet);
				deadLineFirst(cloudlet, slacktime1,vmMips1);
			}
			broker1.submitCloudletList(batchQueBS1);
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
			
			//System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ In ETC matrix !!! @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
			
			for(Cloudlet cloudlet: arrivingCloudlets)
			{
				
				if(cloudlet.getTaskType() == 1) {

					check = -1.00;
					checkBS = -1;
					
					for(int p=0;p<3;p++) {
						if (p==0) {
						
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1));
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2));
						}
						else if(p==2) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3));
				
						}
						
						System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
					}
					
					if(checkBS == 0 && temProbability[checkBS]!= 0.0 && temProbability[checkBS] > dropValue) {
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
						numTaskAllocatedBS1++;
					}
					else if(checkBS==1 && temProbability[checkBS]!= 0.0 && temProbability[checkBS] > dropValue)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker2.getId());
						batchQueBS2.add(cloudlet);
						numTaskAllocatedBS2++;
						
					}
					else if(checkBS==2 && temProbability[checkBS]!= 0.0 && temProbability[checkBS] > dropValue)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker3.getId());
						batchQueBS3.add(cloudlet);
						numTaskAllocatedBS3++;
						
					}
					else {
						//cloudlet.setCloudletStatus(0);
						noTaskNotAllocated++;
					}
	
				}
				
				else if(cloudlet.getTaskType() == 2) {
					
					check = -1.00;
					checkBS = -1;

					for(int p = 0; p < 3; p++) { // calculating the probability and store the highest probability
						if (p==0) {
							
							temProbability[p] = matrix.getProbability(cloudlet.getTaskType(), p, Deadline(cloudlet,matrix,slacktime1));
						}
						else if(p==1) {
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime2));//Deadline(cloudlet,p,matrix,slacktime2)
						}
						else if(p==2) {
							
							convMu = matrix.getMu(cloudlet.getTaskType(), p) + ettmatrix.getMu(cloudlet.getTaskType(), p);
							convSigma = Math.sqrt((Math.pow(matrix.getSigma(cloudlet.getTaskType(), p), 2) + Math.pow(ettmatrix.getSigma(cloudlet.getTaskType(), p), 2)));
							
							temProbability[p] = getConvolveProbability(convMu, convSigma, Deadline(cloudlet,matrix,slacktime3));//Deadline(cloudlet,p,matrix,slacktime3)
				
						}
						
						System.out.println(" probabilities of task type"+ cloudlet.getTaskType() +" in Base Stations "+ p+ "  is : " + temProbability[p] );
						
						if(temProbability[p] > check) {
							
							check = temProbability[p];
							checkBS = p;
						}
					} // end of probability calculation
					
					if(checkBS==0 && temProbability[checkBS]!= 0.0 && temProbability[checkBS] > dropValue) {
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker1.getId());
						batchQueBS1.add(cloudlet);
						//update etc ett
						
						numTaskAllocatedBS1++;
					}
					else if(checkBS==1 && temProbability[checkBS]!= 0.0 && temProbability[checkBS] > dropValue)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker2.getId());
						batchQueBS2.add(cloudlet);
						numTaskAllocatedBS2++;
						
					}
					else if(checkBS==2 && temProbability[checkBS]!= 0.0 && temProbability[checkBS] > dropValue)
					{
						System.out.println(" Highest Probability @@@@@@@@@@@@@@@##########"+temProbability[checkBS] + " Base Station ##########"+checkBS);
						cloudlet.setUserId(broker3.getId());
						batchQueBS3.add(cloudlet);
						numTaskAllocatedBS3++;
						
					}
					else {
						cloudlet.setCloudletStatus(0);
						noTaskNotAllocated++;
						
					}
				
				}
			}// END OF FOR LOOP
			
			broker1.submitCloudletList(batchQueBS1); 
			broker2.submitCloudletList(batchQueBS2); 
			broker3.submitCloudletList(batchQueBS3);
		
		}// end of else
		
		
		
	}
	
	
	
	
	private static void noLoadBalancer(List<Cloudlet> arrivingCloudlets, int vmMips1, int vmMips2,int vmMips3, DatacenterBroker broker1, DatacenterBroker broker2, DatacenterBroker broker3, ETCmatrix matrix,ETTmatrix ettmatrix, int trial  ) throws Exception {
	
	
		List<Cloudlet> batchQueBS1 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS2 = new ArrayList<Cloudlet>();
		List<Cloudlet> batchQueBS3 = new ArrayList<Cloudlet>();
		
		double slacktime1 = 0.15;
		double slacktime2 = 0.25;
		double slacktime3 = 0.17;
		
		
		if(trial==0) {

			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker2.getId());
				batchQueBS2.add(cloudlet);
				deadLineFirst(cloudlet, slacktime2,vmMips2);
			}
				
			broker2.submitCloudletList(batchQueBS2);
		
		}
		else if(trial==1) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker1.getId());
				batchQueBS1.add(cloudlet);
				deadLineFirst(cloudlet, slacktime1,vmMips1);
			}
			broker1.submitCloudletList(batchQueBS1);
			
		}
		else if(trial==2) {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				cloudlet.setUserId(broker3.getId());
				batchQueBS3.add(cloudlet);
				deadLineFirst(cloudlet, slacktime3,vmMips3);
			}
			broker3.submitCloudletList(batchQueBS3);
		
		}
		else {
			
			for(Cloudlet cloudlet:arrivingCloudlets) {
				Deadline(cloudlet,matrix,0.01);
			}
		
			broker1.submitCloudletList(arrivingCloudlets);
			//broker1.submitCloudletList(cloudletList3);
		
		}
	
	}
	

     /**
	 * Creates main() to run the example
	 * @throws IOException 
	 */
	
	
	public static void main(String[] args) throws IOException {
		Log.printLine("Starting Simulation for V2I task processing...");
		int number = 0;
		int arrivingTasks = 0;
		int lbOption = -1;
		//boolean lbOnOff = false;
		String SchPolicy=null;
		taskDstrList = new ArrayList<Cloudlet>();
		
		
		FileWriter fw = new FileWriter("ResultV2I.txt",true);
		PrintWriter printWriter = new PrintWriter(fw);
		printWriter.println();
		printWriter.println("Starting Simulation for V2I task processing...");
		printWriter.println();
		printWriter.println("100 trials, 100 clouldets and Using No load Balancer");
		
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
		printWriter.println();
		
		/* Trial loop */
		
		for(int i = 0;i <number;i++) {
		try {
			printWriter.println("Trial No : "+ i);
			
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
			Datacenter datacenter2 = createDatacenter("BaseStation_2",1,-2.5,50,54000000); /* Base Station x  with coordinates -2.5 and the range of 50 meters. */
			//////End Of Base Station Creation//////
			
			
			/* Third step: Create Brokers.*/
			
			DatacenterBroker broker1 = createBroker("broker1");
			int vmMips1 = 1500;
			vmlist1 = createVM_N(broker1.getId(), 5,vmMips1, 1);
			broker1.submitVmList(vmlist1);
			broker1.setSchedulerPolicy(SchPolicy);
			
			DatacenterBroker broker2 = createBroker("broker2");
			int vmMips2 = 2000;
			vmlist2 = createVM_N(broker2.getId(), 5,vmMips2, 1001);
			broker2.submitVmList(vmlist2);
			broker2.setSchedulerPolicy(SchPolicy);
			
			DatacenterBroker broker3 = createBroker("broker3");
			int vmMips3 = 2500;
			vmlist3 = createVM_N(broker3.getId(), 5,vmMips3, 2001);
			broker3.submitVmList(vmlist3);
			broker3.setSchedulerPolicy(SchPolicy);
			
			/* End of broker creation - Razin.*/
		
			int numTasksAdmitted = arrivingTasks;					/* Set the number of tasks arriving to the Base Station. */
			cloudletList2 = createCloudlet(broker1.getId(),(numTasksAdmitted-90),100,2000,1,400);// tasks ranging 1100 to 1500 mi
     		cloudletList3 = createCloudlet(broker1.getId(),(numTasksAdmitted-10),100,2000,100,400); // tasks ranging 1500 to 2000 mi
			
     				
     		numTaskAllocatedBS1 = 0;
    		numTaskAllocatedBS2 = 0;
    		numTaskAllocatedBS3 = 0;
     		
     		if (lbOption == 1) { // load balancer on without task dropping
				loadBalancer(cloudletList2, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
				loadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
				
			}
     		else if(lbOption==2) {
     			loadBalancerWithTaskDropping(cloudletList2, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,0.5);
     			loadBalancerWithTaskDropping(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i,0.5);
     			
     		}
			else {
				noLoadBalancer(cloudletList2, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
				noLoadBalancer(cloudletList3, vmMips1, vmMips2, vmMips3, broker1, broker2, broker3, matrix,ettMatrix,i);
			}
			
			
     		System.out.println("############Clock time before start simulation ::::"+CloudSim.clock());
     		
     		///CloudSim.clock();
     		
			/* Fifth step: Start the simulation. */
			
			CloudSim.startSimulation();
			
			
			/* Final step: Print results when simulation is over. */
			
			List<Cloudlet> newList = broker1.getCloudletReceivedList();
			//List<VTasks> newList1 = broker1.getVTasksReceivedList();
			
			newList.addAll(broker2.getCloudletReceivedList());
			newList.addAll(broker3.getCloudletReceivedList());
			//taskDstrList.addAll(cloudletList2);
			
			for(Cloudlet cloudlet:newList) {
				
				System.out.println(" CLouldet ::::: "+cloudlet.getCloudletId()+"  waiting time  " + cloudlet.getWaitingTime());
				
				
			}
			
			
			taskDstrList.addAll(newList);
			getDistribution(cloudletList3);
			getETTDistribution(cloudletList3, datacenter0, datacenter1, datacenter2);
			
			ettMatrix = new ETTmatrix(taskTypeNum, dataCenterNum, trTimes);
			matrix = new ETCmatrix(taskTypeNum, dataCenterNum, distributions);

			CloudSim.stopSimulation();
			
			System.out.println("#############Clock time after stop simulation ::::"+CloudSim.clock());
				

     		for(Cloudlet cloudlet:cloudletList2) {
     			
     			System.out.println("Completion time ++++++++++++++++ "+(cloudlet.getFinishTime() - cloudlet.getSubmissionTime()));
     			
     		}
     		
            for(Cloudlet cloudlet:cloudletList3) {
     			
     			System.out.println("Completion time ++++++++++++++++ "+(cloudlet.getFinishTime() - cloudlet.getSubmissionTime()));
     		}
			
			
            
            for(Cloudlet cloudlet:newList) {
            	
            }
            
            
			//System.out.println("^^^^^^^^ list of cloudlets^^^^^^^^^^^ "+taskDstrList.size()); 
			 printCloudletList(newList,printWriter);
		
			
			
			printWriter.println();
			System.out.println(" All counter array ::"+ allCounters.toString());
			printWriter.println(" All counter array ::"+ allCounters.toString());
			printWriter.println();
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
			
			
			//double deadLineMissRate = noTaskNotAllocated / (double)numTasksAdmitted;
			//System.out.println("No of task not allocated : "+ noTaskNotAllocated +" Allocation miss rate : "+ deadLineMissRate);
			//printWriter.println("No of task not allocated : "+ noTaskNotAllocated +" Allocation miss rate : "+ deadLineMissRate);
			
			allCounters.add(noTaskNotAllocated);
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
		long linkBandWidth = 54000000;
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
				
				double completionTime = cloudlet.getFinishTime() - cloudlet.getSubmissionTime(); // current completion time
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
	private static void printCloudletList(List<Cloudlet> list, PrintWriter pw) throws IOException {
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
				"Data center ID" + indent + indent+ "VM ID" + indent + indent+"  "+ "Execution Time"+indent+indent +"Task Length(MI)"+indent+"Task Type"+indent+indent+ "Start Time" + indent + "Finish Time"+indent+indent+indent+"Deadline"+indent+indent+indent+"Completion Time"+indent+indent+indent+"DeadLine Miss");
        
		pw.println("Cloudlet ID" + indent + "STATUS" + indent +indent+
				"Data center ID" + indent+indent+ "VM ID" + indent + indent+"  "+ "Execution Time"+indent+indent +"Task Length(MI)"+ indent+"Task Type"+indent+indent+indent+ "Start Time" + indent + "Finish Time"+indent+indent+indent+"Deadline"+indent+indent+indent+"Completion Time"+indent+indent+indent+"DeadLine Miss");
		
		
		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);
			
			pw.print(indent + cloudlet.getCloudletId() + indent + indent);

			String str=null;
			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
				Log.print("SUCCESS");
				pw.print("SUCCESS");
				
				if(cloudlet.getDeadline()<(cloudlet.getFinishTime() - cloudlet.getSubmissionTime())) {
					str = "False";
					counter++;
				}
				else {
					str ="True";
				}
				
				Log.printLine( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent +indent+indent+indent +cloudlet.getCloudletLength()+ indent + indent +indent+indent+ cloudlet.getTaskType()+indent+indent+indent+ indent+dft.format(cloudlet.getExecStartTime())+ indent + indent+indent + dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+cloudlet.getDeadline()+indent+indent+indent+(cloudlet.getFinishTime() - cloudlet.getSubmissionTime())+indent+indent+indent+str);
				
		
				pw.println( indent + indent+cloudlet.getResourceName(cloudlet.getResourceId()) + indent + indent + indent + cloudlet.getVmId() +
						indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
						indent + indent+indent+indent+indent + cloudlet.getCloudletLength()+ indent + indent +indent+indent+cloudlet.getTaskType()+indent +indent+indent+indent +dft.format(cloudlet.getExecStartTime())+ indent + indent + indent+dft.format(cloudlet.getFinishTime())+indent+indent+indent+indent+cloudlet.getDeadline()+indent+indent+indent+(cloudlet.getFinishTime() - cloudlet.getSubmissionTime())+indent+indent+indent+str);
			}
			
		}
		
		allCounters.add(counter);
		Log.printLine("Number of task missed deadline  : "+counter);
		pw.println("Number of task missed deadline  : "+counter);
		
		
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

