/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import rl.IntHolder;
import rl.RuleInfo;
import rl.RuleLearner;
import utilities.MemoryHistogramer;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;
import evaluations.RuleGroupChiSquare;


/**
 * Implementation of multi-thread LORD algorithm
 * </br>Parallel version for the approach of searching for a locally optimal rule for each training example
 */
public class Lord extends RuleLearner{
	public RuleManager rm;
	
    public Lord(){
        super();
    }
    
    ///////////////////////////////////////////// LEARNING PHASE //////////////////////////////////////////////    
    public long learning(METRIC_TYPES metric_type, double arg){
    	if(this.row_count * this.attr_count > 1000000 * 20){
    		MemoryHistogramer.force_garbage_collection();	// Force collect memory of PPC-tree
    	}
    	
    	long start = System.currentTimeMillis();
    	
    	// Threads
        IntHolder globalIndex = new IntHolder(0);
        Thread[] threads = new Thread[this.thread_count];
    	
		List<Map<String, RuleInfo>> ruleSet_list = new ArrayList<Map<String, RuleInfo>>(this.thread_count);
		for(int i=0; i<this.thread_count; i++){
			Map<String, RuleInfo> rule_set = new HashMap<String, RuleInfo>();
			ruleSet_list.add(rule_set);
			
			threads[i] = new SearchRuleThread(this.selectorID_records,
											this.selector_nlists,
											this.selector_nlist_map,
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
		/*Map<String, RuleInfo> before_filter_rules = new HashMap<String, RuleInfo>();
		for(Map<String, RuleInfo> set : ruleSet_list){
			before_filter_rules.putAll(set);
		}
		System.out.println("Total before-filter rules: " + before_filter_rules.size());
		//for(RuleInfo rule : before_filter_rules.values()){
		//	System.out.println(rule.content());
		//}
		System.out.println("----------------End before-filter rule set-----------------");*/
		
		
		// Build the RuleManager
		this.rm = new RuleManager(this.default_classID, ruleSet_list, this.selectorID_records, this.thread_count);
		    	
    	return System.currentTimeMillis()-start;
    }
    
    ///////////////////////////////////////////// PREDICTION PHASE //////////////////////////////////////////////
    public int[] predict(String[] value_record, IntHolder predicted_classID){
    	return predict_byLORD(value_record, predicted_classID);
    }
    
    /**
     * Native prediction method of LORD
     * @param value_record
     * @param predicted_classID
     * @return
     */
    private int[] predict_byLORD(String[] value_record, IntHolder predicted_classID){
    	int[] id_buffer = new int[this.attr_count];
		int[] example;
		
    	// convert value_record to a record of selectorIDs
		example = this.convert_values_to_selectorIDs(value_record, id_buffer);
		
		if(example.length < 2) {
			// the new example is without body, just its class
			predicted_classID.value = this.rm.defaultClassID;
			
			// To print prediction details
			this.rm.selected_rule = null;
			this.rm.covering_rules = new ArrayList<RuleInfo>();
			
			return example;
		}
		
		Arrays.sort(example);
		RuleInfo best_rule = this.rm.get_best_covering_rule(example); // covering rules stored in rm.covering_rules
		if(best_rule != null){
			predicted_classID.value = best_rule.headID;
			return example;
		}
		
		predicted_classID.value = this.rm.defaultClassID;
		
		return example;
    }
    
    /**
     * Prediction method of CMAR based on Weighted Chi-Square.
     * @param value_record
     * @param predicted_classID
     * @return
     */
    @SuppressWarnings("unused")
	private int[] predict_byWCS(String[] value_record, IntHolder predicted_classID){
    	int[] id_buffer = new int[this.attr_count];
		int[] example;
		
    	// convert value_record to a record of selectorIDs
		example = this.convert_values_to_selectorIDs(value_record, id_buffer);
		
		if(example.length < 2) {
			// the new example is without body, just its class
			predicted_classID.value = this.rm.defaultClassID;
			
			// To print prediction details
			this.rm.selected_rule = null;
			this.rm.covering_rules = new ArrayList<RuleInfo>();
			
			return example;
		}
		
		Arrays.sort(example);
		
		List<RuleInfo> covering_rules = this.rm.ruleTree.find_covering_rules(example);
		
		// no covering rule, use default class
		if(covering_rules.size() == 0){
			predicted_classID.value = this.rm.defaultClassID;
			return example;
		}
		
		// group rules
		Map<Integer, List<RuleInfo>> rule_groups = new HashMap<Integer, List<RuleInfo>>();
		for(RuleInfo rule : covering_rules){
			List<RuleInfo> group = rule_groups.get(rule.headID);
			if(group != null) group.add(rule);
			else{
				group = new ArrayList<RuleInfo>();
				group.add(rule);
				rule_groups.put(rule.headID, group);
			}
		}
		
		// one rule group
		if(rule_groups.size() < 2){
			predicted_classID.value = covering_rules.get(0).headID;
			return example;
		}
		
		// more than one rule group
		double best_chi_square = -Double.MAX_VALUE;
		int best_class_id = -1;
		for(Entry<Integer, List<RuleInfo>> entry : rule_groups.entrySet()){
			double chi_square = RuleGroupChiSquare.calcWeightedChiSquare(entry.getValue(), this.constructing_selectors, this.row_count);
			if(best_chi_square < chi_square){
				best_chi_square = chi_square;
				best_class_id = entry.getKey();
			}
		}
		predicted_classID.value = best_class_id;
		return example;
    }
}
