/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.Comparator;
import java.util.List;

/**
 * Sort decreasingly rules based on heuristic values and then true positive values.
 */
public class RuleComparator implements Comparator<RuleInfo>{

	@Override
	public int compare(RuleInfo rule1, RuleInfo rule2) {
		if(rule2.heuristic_value > rule1.heuristic_value) return 1;
		if(rule2.heuristic_value < rule1.heuristic_value) return -1;
		return (int) (rule2.p - rule1.p);
	}
	
	/**
     * Select the best rule in a non empty list of rules
     * @param candidate_rules
     * @return the best rule
     */
    public static RuleInfo select_best_rule(List<RuleInfo> candidate_rules){
    	RuleInfo chosen_rule = candidate_rules.get(0);
    	for(RuleInfo rule : candidate_rules){
    		if(chosen_rule.heuristic_value < rule.heuristic_value || 
    				(chosen_rule.heuristic_value == rule.heuristic_value && chosen_rule.p < rule.p)){
    			chosen_rule = rule;
    		}
    	}
    	return chosen_rule;
    }
    
    /**
     * Select the best rule in a non empty list of rules
     * @param candidate_rules
     * @return the better one
     */
    
    /**
     * Select the better rule
     * @param rule1 a rule, not null
     * @param rule2 a rule, not null
     * @return the better rule
     */
    public static RuleInfo select_better_rule(RuleInfo rule1, RuleInfo rule2){
    	if(rule1.heuristic_value < rule2.heuristic_value || 
				(rule1.heuristic_value == rule2.heuristic_value && rule1.p < rule2.p)){
			return rule2;
		}else return rule1;
    }
}
