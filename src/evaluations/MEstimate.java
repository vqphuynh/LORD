/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class MEstimate implements HeuristicMetric{
	
	/**
	 * m-Estimate metric: (p + m*P/(P+N)) / (p+n+m)
	 */
	public MEstimate(){
		
	}
	
	public double evaluate(double[] args) {
		return (args[1] + args[6]*args[4]/args[3])/(args[0] + args[6]);
	}
}