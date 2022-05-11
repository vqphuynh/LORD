/*
 * @author Florian Beck, FAW JKU
 *
 */

package rl.eg;

import evaluations.HeuristicMetricFactory.METRIC_TYPES;
import rl.IntHolder;
import rl.RuleInfo;
import rl.RuleLearner;

import java.util.*;
import java.util.stream.IntStream;


/**
 * Implementation of multi-thread LORD algorithm
 * </br>Parallel version for the approach of searching for a locally optimal rule for each training example
 */
public class CNFLord extends RuleLearner{
	public RuleManager rm;

    public CNFLord(){
        super();
		cnf_mode = true;
    }
    
    ///////////////////////////////////////////// LEARNING PHASE //////////////////////////////////////////////    
    public long learning(METRIC_TYPES metric_type, double arg){
    	long start = System.currentTimeMillis();
    	
    	// Threads
        IntHolder globalIndex = new IntHolder(0);
        Thread[] threads = new Thread[this.thread_count];
    	
		List<Map<String, RuleInfo>> ruleSet_list = new ArrayList<>(this.thread_count);
		for(int i=0; i<this.thread_count; i++){
			Map<String, RuleInfo> rule_set = new HashMap<>();
			ruleSet_list.add(rule_set);
			
			threads[i] = new SearchRuleThread(this.selectorID_records,
											this.constructing_selectors,
											this.selector_nodelist_map,
											rule_set,
											metric_type,
											arg,
											globalIndex, i);
			
			threads[i].start();
		}
		
		try {
			for(int i=0; i<this.thread_count; i++) threads[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// How many before-filtered rules are there? uncomment the below code block
		Map<String, RuleInfo> before_filter_rules = new HashMap<>();
		for(Map<String, RuleInfo> set : ruleSet_list){
			before_filter_rules.putAll(set);
		}
		System.out.println("Total before-filter rules: " + before_filter_rules.size());
		
		// Build the RuleManager
		this.rm = new RuleManager(this.default_classID, ruleSet_list, this.selectorID_records, this.thread_count);
    	
    	return System.currentTimeMillis()-start;
    }
    
    ///////////////////////////////////////////// PREDICTION PHASE //////////////////////////////////////////////
    public int[] predict(String[] value_record, IntHolder predicted_classID){
    	int[] id_buffer = IntStream.range(0, selector_count).toArray();
		int[] example;
		
    	// convert value_record to a record of selectorIDs
		// TODO
		example = this.convert_values_to_selectorIDs(value_record, id_buffer.clone());
		
		if(example.length < 2) {
			// the new example is without body, just its class
			predicted_classID.value = this.rm.defaultClassID;
			
			// To print prediction details
			this.rm.selected_rule = null;
			this.rm.covering_rules = new ArrayList<>();
			
			return example;
		}
		
		Arrays.sort(example);
		RuleInfo best_rule = this.rm.get_best_covering_rule(example);
		if(best_rule != null){
			predicted_classID.value = best_rule.headID;
			return example;
		}
		
		predicted_classID.value = this.rm.defaultClassID;
		
		return example;
    }
}
