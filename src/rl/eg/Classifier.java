/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rl.IntHolder;
import rl.RTree;
import rl.RuleComparator;
import rl.RuleInfo;

public class Classifier {
	public int defaultClassID;
	public List<RuleInfo> ruleList;
	public List<RuleInfo> truncatedRuleList;
	public RTree ruleTree;
	
	public List<RuleInfo> covering_rules = null;
	public RuleInfo selected_rule = null;
	
	public RuleInfo get_best_covering_rule(int[] example){
		this.covering_rules = this.ruleTree.find_covering_rules(example);
		
		if(covering_rules.size()==0){
			return (selected_rule = null);
		}
		
		return (selected_rule = this.select_best_rule());
	}
	
	private RuleInfo select_best_rule(){
    	RuleInfo chosen_rule = covering_rules.get(0);
    	for(RuleInfo rule : covering_rules){
    		if(chosen_rule.heuristic_value < rule.heuristic_value || 
    				(chosen_rule.heuristic_value == rule.heuristic_value && chosen_rule.p < rule.p)){
    			chosen_rule = rule;
    		}
    	}
    	return (selected_rule = chosen_rule);
    }
	
	/**
	 * Sort rules in the rule list decreasingly based on heuristic values and then true positive values.
	 */
	public void sort_rules(){
		RuleComparator c = new RuleComparator();
		this.ruleList.sort(c);
	}
	
	/**
	 * Truncate the last portion of the rules in the rule list and rebuild the rTree
	 * @param portion the portion of rules to be truncated
	 */
	public void truncate(double portion){
		int remain = (int) (this.ruleList.size()*(1-portion));
		this.truncatedRuleList = new ArrayList<RuleInfo>(remain);
		for(int i=0; i<remain; i++) this.truncatedRuleList.add(this.ruleList.get(i));
		
		//rebuild the rTree
		this.ruleTree = new RTree();
		for(RuleInfo rule : this.truncatedRuleList){
			this.ruleTree.insert_rule_inverse_order(rule.body, rule);
		}
	}
	
	/////////////////////////////////////////////////////// CONSTRUCTORS ///////////////////////////////////////////////////////
	
	public Classifier(int default_class_id, Map<String, RuleInfo> rule_set){
		this.defaultClassID = default_class_id;
		
		List<RuleInfo> rule_list = new ArrayList<RuleInfo>(rule_set.size());
		for(RuleInfo rule : rule_set.values()) rule_list.add(rule);
		RuleComparator c = new RuleComparator();
		rule_list.sort(c);
		int remain_count = (int) (rule_list.size()*0.75);
		
		// Build the corresponding RTree
		this.ruleTree = new RTree();
		this.ruleList = new ArrayList<RuleInfo>(remain_count);
		RuleInfo rule;
		for(int i=0; i<remain_count; i++){
			rule = rule_list.get(i);
			this.ruleTree.insert_rule_inverse_order(rule.body, rule);
			this.ruleList.add(rule);
		}
	}
	
	public Classifier(int default_class_id, Map<String, RuleInfo> rule_set, int[][] selectorID_records){
		this.defaultClassID = default_class_id;
		
		// Build the corresponding RTree
		this.ruleTree = new RTree();
		for(RuleInfo rule : rule_set.values()){
			this.ruleTree.insert_rule_inverse_order(rule.body, rule);
		}
		
		// Filter rules		
		Map<String, RuleInfo> filtered_rule_set = new HashMap<String, RuleInfo>();
		
		RuleInfo init_rule = new RuleInfo();
		init_rule.heuristic_value = -Double.MAX_VALUE;

		for(int[] example : selectorID_records){
			// selector IDs in the example are already sorted
			int classID = example[example.length-1];
			List<RuleInfo> covering_rules = this.ruleTree.find_covering_rules(example);
			
			RuleInfo selected_rule = init_rule;
			
			for(RuleInfo rule : covering_rules){
				if(rule.headID == classID && (selected_rule.heuristic_value < rule.heuristic_value || 
	    				(selected_rule.heuristic_value == rule.heuristic_value && selected_rule.p < rule.p))){
					selected_rule = rule;
				}
	    	}
			
			if(selected_rule != init_rule){
				filtered_rule_set.put(selected_rule.signature(), selected_rule);
			}
		}
		
		// Build the corresponding RTree
		this.ruleTree = new RTree();
		this.ruleList = new ArrayList<RuleInfo>(filtered_rule_set.size());
		for(RuleInfo rule : filtered_rule_set.values()){
			this.ruleTree.insert_rule_inverse_order(rule.body, rule);
			this.ruleList.add(rule);
		}
	}
	
	public Classifier(int default_class_id,
						List<Map<String, RuleInfo>> ruleSet_list,
						int[][] selectorID_records,
						int thread_count){
		this.defaultClassID = default_class_id;
		
		// Build the corresponding RTree
		RTree tmp_tree = new RTree();
		for(Map<String, RuleInfo> rule_set : ruleSet_list){
			for(RuleInfo rule : rule_set.values()){
				tmp_tree.insert_rule_inverse_order(rule.body, rule);
			}
		}
		
		// Filter rules
		IntHolder globalIndex = new IntHolder(0);
        Thread[] threads = new Thread[thread_count];
		List<Map<String, RuleInfo>> filtered_rule_sets = new ArrayList<Map<String, RuleInfo>>(thread_count);
		
		for(int i=0; i<thread_count; i++){
			Map<String, RuleInfo> filtered_rule_set = new HashMap<String, RuleInfo>();
			filtered_rule_sets.add(filtered_rule_set);
			
			threads[i] = new FilterRuleThread(tmp_tree,
											selectorID_records,
											filtered_rule_set,
											globalIndex, i);
			
			threads[i].start();
		}
		
		try {
			for(int i=0; i<thread_count; i++) threads[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Build the corresponding RTree
		int rule_count = 0;
		for(Map<String, RuleInfo> rule_set : filtered_rule_sets) rule_count += rule_set.size();
		Map<String, RuleInfo> ruleSet = new HashMap<String, RuleInfo>(rule_count);
		for(Map<String, RuleInfo> rule_set : filtered_rule_sets) ruleSet.putAll(rule_set);
		
		this.ruleTree = new RTree();
		this.ruleList = new ArrayList<RuleInfo>(ruleSet.size());
		for(RuleInfo rule : ruleSet.values()){
			this.ruleTree.insert_rule_inverse_order(rule.body, rule);
			this.ruleList.add(rule);
		}
	}
	
	public Classifier(int default_class_id,
						List<Map<String, RuleInfo>> ruleSet_list,
						int max_rule_length){
		this.defaultClassID = default_class_id;
		
		// Group rules by their lengths
		List<List<RuleInfo>> rule_groups = new ArrayList<List<RuleInfo>>(max_rule_length);
		for(int i=1; i<max_rule_length; i++) rule_groups.add(new ArrayList<RuleInfo>());
		
		for(Map<String, RuleInfo> rule_set : ruleSet_list){
			for(RuleInfo rule : rule_set.values()){
				rule_groups.get(rule.body.length).add(rule);
			}
		}
		
		// Build the corresponding RTree and filter rules
		this.ruleTree = new RTree();
		for(List<RuleInfo> rule_list : rule_groups){
			for(RuleInfo rule : rule_list){
				this.ruleTree.insert_rule_inverse_order_withfilter(rule.body, rule);
			}
		}
		
		this.ruleList = this.ruleTree.get_rule_list();
	}
	
}
