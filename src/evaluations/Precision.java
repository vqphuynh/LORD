/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class Precision implements HeuristicMetric{
	
	/**
	 * Precision metric p/(n+p)
	 */
	public Precision(){
		
	}
	
	public double evaluate(double[] args) {
		return args[1]/args[0];
	}
}
