package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class zScoreTable {
	
	
	public static void main(String[] args)throws Exception
	  {
	  // We need to provide file path as the parameter:
	  // double backquote is to avoid compiler interpret words
	  // like \test as \t (ie. as a escape sequence)
		
	  Double ar1[][] = new Double[40][10];
	  String[] words = null;
		
	  File file = new File("/home/c00303945/zsore_negative.txt");
	 
	  BufferedReader br = new BufferedReader(new FileReader(file));
	 
	  String st;
	  int i=0;
	  int j;
	  while ((st = br.readLine()) != null) {
		  
	  words = st.split("\\s");	  
	  j=0;    
	  for(String w:words){  
		  
		  System.out.print(w + " ");
		  ar1[i][j] = Double.parseDouble(w);
		  j++;
		  }  
	  System.out.println();
	  i++;
	  }
	  
	  
//	  for(int r = 0;i<40;i++) {
//			 for(int c=0;c<10;c++) {
//				 
//				 System.out.print(" " + ar1[r][c] + " ");
//				 
//			 }
//			 System.out.println();
//			 
//		 }
//	  
	  
	  
	  }
	
	
	

}
