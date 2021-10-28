/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

/**
 * The metric takes general parameters:
 * </br>args[0] = TruePositive + FalsePositive = p + n = sup(Body)
 * </br>args[1] = TruePositive = p = sup(Body + Head)
 * </br>args[2] = FalsePositive = n = sup(Body + not Head)
 * </br>args[3] = Total examples = |D| = P + N
 * </br>args[4] = Positive = P = sup(Head)
 * </br>args[5] = Negative = N = sup(not Head)
 * </br>args[6] = parameter (e.g. c, cr, m of cost, relative cost, m-Estimate)
 */
public interface HeuristicMetric {
	
	/**
	 * The metric takes general parameters:
	 * </br>args[0] = TruePositive + FalsePositive = p + n = sup(Body)
	 * </br>args[1] = TruePositive = p = sup(Body + Head)
	 * </br>args[2] = FalsePositive = n = sup(Body + not Head)
	 * </br>args[3] = Total examples = |D| = P + N
	 * </br>args[4] = Positive = P = sup(Head)
	 * </br>args[5] = Negative = N = sup(not Head)
	 * </br>args[6] = parameter (e.g. c, cr, m of cost, relative cost, m-Estimate)
	 */
	public double evaluate(double[] args);
}
