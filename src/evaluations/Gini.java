/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class Gini implements HeuristicMetric{
	
	/**
	 * Gini metric p*n/(n+p)*(n+p)
	 */
	public Gini(){
		
	}
	
	public double evaluate(double[] args) {		
		return args[1]*args[2]/(args[0]*args[0]);
	}
}
