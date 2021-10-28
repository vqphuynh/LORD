/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class RelativeCost implements HeuristicMetric{
	
	/**
	 * Relative Cost metric: cr*(p/P) - (1-cr)*(n/N)
	 */
	public RelativeCost(){
		
	}
	
	public double evaluate(double[] args) {
		return args[6]*args[1]/args[4] - (1 - args[6])*args[2]/args[5];
	}
}