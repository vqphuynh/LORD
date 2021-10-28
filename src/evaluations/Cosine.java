/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class Cosine implements HeuristicMetric{
	
	/**
	 * Cosine metric: p/sqrt((n+p)*P)
	 */
	public Cosine(){
		
	}
	
	public double evaluate(double[] args) {
		return args[1]/Math.sqrt(args[0]*args[4]);
	}
}