package org.cloudbus.cloudsim.examples;

public class percentageCal {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		double a[] = {1.746424919657298, 2.3769728648009427, 1.4177446878757824, 2.4677925358506134, 4.964876634922564, 5.932958789676531, 6.449806198638839, 9.282241108697836, 7.764663547121664, 10.044401425669923};
		
		
		for(int i=0;i<a.length;i++) {
			
			
			
			//a[i] = (a[i]/ 500+(500*(i+1)))*100;
			
			a[i] = a[i] / (100*(i+1));
			a[i] = a[i]*100;
			
			
		}
		
     for(int i=0;i<a.length;i++) {
			
			System.out.print(a[i]+", ");
			
		}
		

	}

}
