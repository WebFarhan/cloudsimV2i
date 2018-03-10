package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.math3.distribution.NormalDistribution;

public class ETTmatrix {


	/**
	 *  The matrix is holding a mean and a standard deviation 
	 *  for each type of task on every base station
	 */
	protected NormalDistribution[][] etcMatrix = null;
	
	/**
	 * The number of Virtual Machines in the simulation and
	 * the number of Task Types
	 */
	protected int totalDataC = 0;
	protected int totalCloudlet = 0;
	
	protected HashMap<String, NormalDistribution> distributions;

	private String[] arrays;
	
	/**
	 * A private constructor to ensure that only 
	 * an correct initialized matrix could be created
	 */
	
	@SuppressWarnings("unused")
	private ETTmatrix() {
		
	};
	
	/**
	 * A parameterized constructor
	 * @param vmTotalNum takes the total number of VMs in the simulation 
	 * @param cloudletTotalnum takes the total number of Cloudlet Types in the simulation
	 */
	
	public ETTmatrix(int taskTypeTotal, int dataCnum, HashMap<String, NormalDistribution> _distributions) {
		
		this.totalDataC = dataCnum;
		this.totalCloudlet = taskTypeTotal;
		this.etcMatrix = new NormalDistribution[taskTypeTotal+1][dataCnum];
		this.distributions = _distributions;
		
		for(String key: distributions.keySet()) {
			
			arrays = key.split("\\.");
			
			int row = Integer.parseInt(arrays[0]);
			int column = Integer.parseInt(arrays[1]);
			NormalDistribution newDistribution = distributions.get(key);
			this.etcMatrix[row][column] = newDistribution;
			
			
			}
		/*for(int i = 0; i < taskTypeTotal; i ++) {
				for(int j = 0; j < dataCnum; j++ ) {
					if(this.etcMatrix[i][j] == null) {
						normalDistr nullDistr = new normalDistr(0.0, 0.0);
						this.etcMatrix[i][j] = nullDistr;
					}
				}
			}*/
		}
		
	

	
	/**
	 * Returns the LognormalDistr object
	 * @param dataCenterID
	 * @param cloudletType
	 * @return
	 */
	
	/*public NormalDistribution getDistribution(int cloudletType, int dataCenterID) {
			
			if (cloudletType > totalCloudlet || dataCenterID > totalDataC) {
				throw new ArrayIndexOutOfBoundsException("The Virtual Machine or the Task Type does not exist in this ETC");
			}
			if(etcMatrix[cloudletType][dataCenterID] == null) {
				normalDistr nullDistr = new normalDistr(0.0, 0.0);
				return nullDistr;
			}

			return etcMatrix[cloudletType][dataCenterID];
		}*/

		/**
		 * Inputs the LognormalDistributions from the HashMap into
		 * the matrix (2d-array)
		 */
	

	public double getMu(int taskType, int DataCenter) {
		
		if(etcMatrix[taskType][DataCenter] == null) {
			return 0.0;
		}
		
		NormalDistribution distr = etcMatrix[taskType][DataCenter];
		
		return distr.getMean();
	}
	
	public double getSigma(int taskType, int DataCenter) {
		
		if(etcMatrix[taskType][DataCenter] == null) {
			return 0.0;
		}
		
		NormalDistribution distr = etcMatrix[taskType][DataCenter];
		
		return distr.getStandardDeviation();
	}
	
	public double getWorseCaseTime(int taskType, int DataCenter) {
		
		if(etcMatrix[taskType][DataCenter] == null) {
			return 0.0;
		}
		
		NormalDistribution distr = etcMatrix[taskType][DataCenter];
		double time = distr.getMean() + distr.getStandardDeviation();
		
		return time;
		
	}
	
	public double getProbability(int taskType, int dataCenter, double deadLine) {
		
		if(etcMatrix[taskType][dataCenter] == null) {
			return 1.0;
		}
		NormalDistribution distr = etcMatrix[taskType][dataCenter];
		//double worstTime = distr.getMean() + distr.getStandardDeviation();
		
		return distr.cumulativeProbability(deadLine);
		
	}
	
	
	
	public void printMatrix() {
		
		System.out.println(Arrays.deepToString(etcMatrix));
		
	}
	
	
}
