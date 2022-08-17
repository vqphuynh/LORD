/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package evaluations;

import java.util.List;

import prepr.Selector;
import rl.RuleInfo;

/**
 * Class for calculating Weighted Chi-Square value of a rule group, proposed in CMAR algorithm.
 */
public class RuleGroupChiSquare {
	
	/**
	 * Calculate weighted Chi-Square for a rule group 
	 * @param rule_group
	 * @param constructing_selectors
	 * @param row_count
	 * @return
	 */
	public static double calcWeightedChiSquare(List<RuleInfo> rule_group, 
											List<Selector> constructing_selectors,
											double row_count) {
	    double wcsValue = 0;
	    
	    for(RuleInfo rule : rule_group){
	    	double chiSquaredValue = calcChiSquaredValue(rule.n_plus_p, 
										    			constructing_selectors.get(rule.headID).frequency,
										    			rule.p, 
										    			row_count);
	    	
			double chiSquaredUB = calcChiSquaredUpperBound(rule.n_plus_p,
														   constructing_selectors.get(rule.headID).frequency,
														   row_count);
			
			wcsValue = wcsValue + (chiSquaredValue*chiSquaredValue)/chiSquaredUB;
	    }
	    
	    return wcsValue;
	}
	
	/**
	 * Calculate and return the Chi-Squared value of a rule
	 * @param supAntecedent
	 * @param supConsequent
	 * @param supRule
	 * @param numRecords the number of records in the training sets.
	 * @return the Chi-Squared value
	 */
    private static double calcChiSquaredValue(double supAntecedent, double supConsequent, 
    								 double supRule, double numRecords){
		// Calculate observed
    	double[] obsValues = new double[4];
    	obsValues[0]=supRule;
		obsValues[1]=supAntecedent-supRule;
		obsValues[2]=supConsequent-supRule;
		obsValues[3]=numRecords-supAntecedent-supConsequent+supRule;
	 
		// Calculate additional support values
		double supNotAntecedent=numRecords-supAntecedent;
    	double supNotConsequent=numRecords-supConsequent;
    	
    	// Calculate expected values
    	double[] expValues = new double[4];
    	expValues[0]=(supConsequent*supAntecedent)/numRecords;
		expValues[1]=(supNotConsequent*supAntecedent)/numRecords;
		expValues[2]=(supConsequent*supNotAntecedent)/numRecords;
		expValues[3]=(supNotConsequent*supNotAntecedent)/numRecords;
		
		// Calculate Chi-Squared value
		double chi_squared_value = 0.0;
        for (int i=0; i<obsValues.length; i++) {
        	double obs_exp_diff = obsValues[i]- expValues[i];
        	chi_squared_value = chi_squared_value + ((obs_exp_diff*obs_exp_diff)/expValues[i]);
	    }
        
		return chi_squared_value;
	}
    
    /**
     * Calculate the upper bound for the Chi-Squared value of a rule.
     * @param suppAnte
     * @param suppCons
     * @param numRecords
     * @return
     */
    private static double calcChiSquaredUpperBound(double suppAnte, double suppCons, double numRecords) {
        double term;
	
		// Test support of antecedent and consequence and choose minimum
		if (suppAnte < suppCons){
			term = suppAnte - ((suppAnte*suppCons)/numRecords);	     
		}
		else{
			term = suppCons-((suppAnte*suppCons)/numRecords);
		}
			      
		// Determine e value
		double eValue = calcWCSeValue(suppAnte, suppCons, numRecords); 
		
		// Return upper bound
		return (term*term*eValue*numRecords);
	}
    
    /**
     * Calculates and returns the e value for calculating Weighted Chi-Squared (WCS) values.
     * @param suppAnte
     * @param suppCons
     * @param numRecords
     * @return
     */
    private static double calcWCSeValue(double suppAnte, double suppCons, double numRecords){
    	double suppNotCons = numRecords - suppCons;
    	double suppNotAnte = numRecords - suppAnte;
    	
    	return (1/(suppAnte*suppCons) + 1/(suppAnte*suppNotCons) + 1/(suppCons*suppNotAnte) + 1/(suppNotAnte*suppNotCons));
	}
	
}
	
