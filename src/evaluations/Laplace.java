/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class Laplace implements HeuristicMetric{
	
	/**
	 * Laplace metric (p+1)/(n+p+2)
	 */
	public Laplace(){
		
	}
	
	public double evaluate(double[] args) {
		return (args[1]+1)/(args[0]+2);
	}
}
