package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;

public class dummyETC {
	
	private static List<Cloudlet> cloudletList1;
	private static double matrix1[][];
	
	private static double mu,sigma;
	
	
	private void storeValue(int totalBS, int totalTT) {
		
		
		matrix1= new double[totalTT][totalBS];
		
		for(int i=0;i<totalTT;i++) {
			for(int j=0;j<totalBS;j++) {
				
				matrix1[i][j]= 0;
			}
		}
		
	}
	
	
	private static void calculateMu() {
		
		
		
		
	}
	
	

	public dummyETC(List<Cloudlet> cloudletList) {
		
		this.cloudletList1 = cloudletList;
	
	}
	
	
}
