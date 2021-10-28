/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class LinearCost implements HeuristicMetric{
	
	/**
	 * Linear Cost metric: c*p - (1-c)*n
	 */
	public LinearCost(){
		
	}
	
	public double evaluate(double[] args) {		
		return args[6]*args[1] - (1 - args[6])*args[2];
	}
}
