/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.distributions;



/**
 * The Class LognormalDistr.
 * 
 * @author Marcos Dias de Assuncao
 * @since CloudSim Toolkit 1.0
 */
public class normalDistr  {

	/** The num gen. */

	/** The mean. */
	private final double mean;

	/** The dev. */
	private final double dev;

	/**
	 * Instantiates a new lognormal distr.
	
	 * @param mean the mean
	 * @param dev the dev
	 */
	public normalDistr(double mean, double dev) {
		
		this.mean = mean;
		this.dev = dev;
	}


	
	public double getMu() {
		
		return mean;
	}
	
	public double getSigma() {
		
		return dev;
	}



}
