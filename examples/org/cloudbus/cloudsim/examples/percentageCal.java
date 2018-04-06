package org.cloudbus.cloudsim.examples;

public class percentageCal {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		double a[] = {1.0, 2.0, 4.0, 4.0, 5.0, 5.0, 6.0, 6.0, 6.0, 6.0};
		
		
		for(int i=0;i<a.length;i++) {
			
			if(i>0) {
			a[i] = (a[i]/(i*100))*100;
			}
			
		}
		
     for(int i=0;i<a.length;i++) {
			
			System.out.print(a[i]+", ");
			
		}
		

	}

}
