/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.Comparator;
import java.util.List;

/**
 * Sort decreasingly rules based on heuristic values, then true positive values, finally classes.
 */
public class RuleComparator implements Comparator<RuleInfo>{

	@Override
	public int compare(RuleInfo rule1, RuleInfo rule2) {
		//Sort decreasingly rules based on heuristic values, then true positive values, finally classes.
		
		// heuristic value
		if(rule2.heuristic_value > rule1.heuristic_value) return 1;
		if(rule2.heuristic_value < rule1.heuristic_value) return -1;
		
		// p value (count of true positive examples)
		if(rule2.p > rule1.p) return 1;
		if(rule2.p < rule1.p) return -1;
		
		// class, the superior class is the one with larger class id.
		//return (rule2.headID - rule1.headID);
		return (rule1.headID - rule2.headID);	// favor the inferior class because of the the same heuristic value and true positive
	}
	
	/**
     * Select the best rule in a non empty list of rules
     * </br>Based on: heuristic value, then true positive value, finally the classes
     * @param candidate_rules
     * @return the best rule
     */
    public static RuleInfo select_best_rule(List<RuleInfo> candidate_rules){
    	RuleInfo chosen_rule = candidate_rules.get(0);
    	for(RuleInfo rule : candidate_rules){
    		if( (chosen_rule.heuristic_value < rule.heuristic_value) || 
    			(chosen_rule.heuristic_value == rule.heuristic_value && chosen_rule.p < rule.p) ||
    			//(chosen_rule.heuristic_value == rule.heuristic_value && chosen_rule.p == rule.p && chosen_rule.headID < rule.headID)){
    			(chosen_rule.heuristic_value == rule.heuristic_value && chosen_rule.p == rule.p && chosen_rule.headID > rule.headID)){ // favor the inferior class
    			chosen_rule = rule;
    		}
    	}
    	return chosen_rule;
    }
    
    /**
     * Select the better rule
     * </br>Based on: heuristic value, then true positive value, finally the classes
     * @param rule1 a rule, not null
     * @param rule2 a rule, not null
     * @return the better rule
     */
    public static RuleInfo select_better_rule(RuleInfo rule1, RuleInfo rule2){
    	if( (rule1.heuristic_value < rule2.heuristic_value) || 
			(rule1.heuristic_value == rule2.heuristic_value && rule1.p < rule2.p) ||
			//(rule1.heuristic_value == rule2.heuristic_value && rule1.p == rule2.p && rule1.headID < rule2.headID)){
    		(rule1.heuristic_value == rule2.heuristic_value && rule1.p == rule2.p && rule1.headID > rule2.headID)){ // favor the inferior class
			return rule2;
		}else return rule1;
    }
}
