package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class VTasks extends Cloudlet{

	//private double cloudletDeadline;
	//The time when cloudlet are created and put in the batch queue, which is different with the
	//the arrival time in ResCloudlet which is the time when cloudlet are queued in vm local queue
	private double arrivalTime;
	private double deadlineTime;
	private int HD;// vehicle movement direction west = 1, north = 2, east = 3, south = 4
	private int taskId; // requested task ID. this can be cloudlet ID
	private double taskPriority;
	private int taskType; // task is delay sensitive or not. delay sensitive = 1 , non delay sensitive = 0
	private int vID; // vehicles ID
	private int ReqID; // Request ID
	
	private double finishTime;
	private int status;
	private boolean record;
	//private int std;
	//private long avglength;
	private int ID;// The id of the requested data item
	private double cloudletDeadline;
	
	
	
	//Create a buffer to store the video gop raw data
	//Buffer buffer = Buffer.make(null, 1000);

    
	
	public VTasks(
			final int cloudletId,
			//final int taskId,
			//final int vID,
			//final int HD,
			//final int taskType,
		    final long cloudletLength,
			//final long avglength,
			//final int std,
			final int arrivalTime,
		    final double deadlineTime,
		    final int HD,
		    final int taskId,
		    final double taskPriority,
		    final int taskType,
		    final int vID,
		    final int ReqID,
			final int pesNumber,
			final long cloudletFileSize,
			final long cloudletOutputSize,
			final UtilizationModel utilizationModelCpu,
			final UtilizationModel utilizationModelRam,
			final UtilizationModel utilizationModelBw) {
		super(
				cloudletId,
				cloudletLength,
				pesNumber,
				cloudletFileSize,
				cloudletOutputSize,
				utilizationModelCpu,
				utilizationModelRam,
				utilizationModelBw,
				false
				);
	   this.arrivalTime = arrivalTime;
	   this.deadlineTime = deadlineTime;
	   this.HD = HD;
	   this.taskId = taskId;
	   this.taskPriority = taskPriority;
	   this.taskId = taskId;
	   //this.vID = vID;
	   //this.HD = HD;
	   //this.taskType = taskType;
	   //this.avglength = avglength;
	   //this.std = std;
		
	}
		
	/*
	public long getAvgCloudletLength(){
		return avglength;
	}
	*/
	/*
	public long getCloudletStd(){
		return std;
	}
	*/
	
	public double getArrivalTime(){
		return arrivalTime;
	}
	
	public double getCloudletDeadline() {
		return cloudletDeadline;
	}
	
	public void setCloudletDeadline(double cloudletDeadline){
		this.cloudletDeadline = cloudletDeadline;
	}
	
	public int getCloudletTaskId() {
		return taskId;
	}

}
