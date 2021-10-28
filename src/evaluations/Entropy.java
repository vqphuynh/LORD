/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class Entropy implements HeuristicMetric{
	
	/**
	 * Entropy metric -( p/(n+p)*log(p/(n+p)) + n/(n+p)*log(n/(n+p)) )
	 */
	public Entropy(){
		
	}
	
	public double evaluate(double[] args) {
		double r1 = args[1]/args[0];
		double r2 = 1 - r1;
		return -(r1*Math.log(r1) + r2*Math.log(r2));
	}
}
