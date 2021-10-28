/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;	// eager greedy

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import rl.IntHolder;
import rl.RuleInfo;
import rl.RuleLearner;
import rl.RuleSearcher;
import evaluations.HeuristicMetric;
import evaluations.HeuristicMetricFactory;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

/**
 * Implementation of LORD algorithm
 * </br>The approach of searching for a locally optimal rule for each training example.
 */
public class Lord extends RuleLearner {
	public Classifier classifier;
	
    ///////////////////////////////////////////////MINING PHASE//////////////////////////////////////////////
    /**
     * @param target_attr_count
     * @throws FileNotFoundException
     */
    public Lord(int target_attr_count) throws FileNotFoundException {
        super(target_attr_count);
    }
    
    /**
     * @param thread_count number of threads to run
     * @param target_attr_count
     * @throws FileNotFoundException
     */
    public Lord(int thread_count, int target_attr_count) throws FileNotFoundException {
    	super(thread_count, target_attr_count);
    }
    
    ///////////////////////////////////////////// LEARNING PHASE //////////////////////////////////////////////    
    public long learning(METRIC_TYPES metric_type, double arg){
    	long start = System.currentTimeMillis();
    	
    	HeuristicMetric metric = HeuristicMetricFactory.getInterestMetric(metric_type);
    	double[] arguments = new double[7];
    	arguments[3] = this.row_count;
    	arguments[6] = arg;
		
		int example_classID;
		RuleInfo greedy_best_rule;
		Map<String, RuleInfo> rule_set = new HashMap<String, RuleInfo>();
		
		for(int[] example : this.selectorID_records){
			if(example.length < 2) continue;
			
			example_classID = example[example.length-1];
			int[] body_selector_IDs = new int[example.length-1];
			System.arraycopy(example, 0, body_selector_IDs, 0, body_selector_IDs.length);
			
			arguments[4] = this.constructing_selectors.get(example_classID).frequency;
			arguments[5] = arguments[3] - arguments[4];
			
			greedy_best_rule = RuleSearcher.search_for_greedy_best_rule_verbose(this.selector_nodelist_map,
																		body_selector_IDs,
																		example_classID,
																		metric,
																		arguments);
			
			rule_set.put(greedy_best_rule.signature(), greedy_best_rule);
		}
		
		// Build the output classifier
		// this.classifier = new Classifier(this.get_default_class(), rule_set);
		this.classifier = new Classifier(this.get_default_class(), rule_set, this.selectorID_records);	
    	
    	return System.currentTimeMillis()-start;
    }
    
    ///////////////////////////////////////////// PREDICTION PHASE //////////////////////////////////////////////
    public int[] predict(String[] value_record, IntHolder predicted_classID){
    	int[] id_buffer = new int[this.attr_count];
		int[] example;
		
    	// convert value_record to a record of selectorIDs
		example = this.convert_values_to_selectorIDs(value_record, id_buffer);
		
		if(example.length < 2) {
			predicted_classID.value = this.classifier.defaultClassID;
			
			// For printing prediction details
			this.classifier.selected_rule = null;
			this.classifier.covering_rules = new ArrayList<RuleInfo>();
			
			return example;
		}
		
		Arrays.sort(example);
		RuleInfo best_rule = this.classifier.get_best_covering_rule(example);
		if(best_rule != null){
			predicted_classID.value = best_rule.headID;
			return example;
		}
		
		predicted_classID.value = this.classifier.defaultClassID;
		
		return example;
    }
}
