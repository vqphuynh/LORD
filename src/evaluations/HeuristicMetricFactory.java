/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

public package evaluations;


public class HeuristicMetricFactory {
	
	public enum METRIC_TYPES {PRECISION, LAPLACE, ENTROPY, MESTIMATE, LINEAR_COST, RELATIVE_COST, COSINE};
	
	public static HeuristicMetric getInterestMetric(METRIC_TYPES metric_type){
		switch(metric_type){
			case PRECISION:
				return new Precision();
			case LAPLACE:
				return new Laplace();
			case ENTROPY:
				return new Entropy();
			case MESTIMATE:
				return new MEstimate();
			case LINEAR_COST:
				return new LinearCost();
			case RELATIVE_COST:
				return new RelativeCost();
			case COSINE:
				return new Cosine();
			default:
				return new Precision();
		}
	}
}
