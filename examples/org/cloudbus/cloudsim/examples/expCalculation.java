package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;

public class expCalculation {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		ArrayList<Integer> values = new ArrayList<>();
		
		double sum = 0;
				
		double[] data = {265, 236, 264, 252, 257, 252, 242, 254, 245, 243, 236, 233, 230, 222, 229, 229, 230, 223, 223, 223, 226, 223, 235, 224, 229, 227, 230, 228, 224};
	
		
		double checkMax=-1;
		double checkMin=200000;
		
		for(Double a:data) {
			sum=sum+a;
			if(a>checkMax) {
				
				checkMax = a;
			}
			
			if(a<checkMin) {
				checkMin = a;
			}
		}
		
		
		System.out.println("Max :" + checkMax);
		System.out.println("Min :" + checkMin);
		
		
		System.out.println(data.length);
		//System.out.println("average is : " + (sum/30));
	}

}
