/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

public class MRelativeLaplace implements HeuristicMetric{
	
	/**
	 * MRelativeLaplace metric: (p/P + m/2) / (p/P + n/N + m)
	 */
	public MRelativeLaplace(){
		
	}
	
	public double evaluate(double[] args) {
		double p_P_ratio = args[1] / args[4];
		
		return (p_P_ratio + args[6]/2) / (p_P_ratio + args[2]/args[5] + args[6]);
	}
}