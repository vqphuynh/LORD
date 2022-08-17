/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;

import java.util.List;
import java.util.Map;

import rl.IntHolder;
import rl.RTree;
import rl.RuleInfo;

class FilterRuleThread extends Thread{
	private RTree rtree;
	private int[][] selectorID_records;
	private Map<String, RuleInfo> filtered_rule_set;
	private IntHolder globalIndex;
	private int id;
	
	public FilterRuleThread(RTree tree,
						int[][] selectorID_records,
						Map<String, RuleInfo> filtered_rule_set,
						IntHolder globalIndex,
						int id){
		this.rtree = tree;
		this.selectorID_records = selectorID_records;
		this.filtered_rule_set = filtered_rule_set;
		this.globalIndex = globalIndex;
		this.id = id;
	}
	
	public void run(){
		long start = System.currentTimeMillis();
		int row_count = this.selectorID_records.length;
		int[] example;
		
		RuleInfo init_rule = new RuleInfo();
		init_rule.heuristic_value = -Double.MAX_VALUE;
		
		while (true){
			synchronized(globalIndex){
				if(this.globalIndex.value >= row_count) break;
				example = this.selectorID_records[this.globalIndex.value];
				this.globalIndex.value++;
			}
			
			// selector IDs in the example are already sorted
			int classID = example[example.length-1];
			List<RuleInfo> covering_rules = this.rtree.find_covering_rules(example);
			
			RuleInfo selected_rule = init_rule;
			
			for(RuleInfo rule : covering_rules){
				// Do not need to compare on classes, because we only examine on full covering rules (match the rule head)
				if(rule.headID == classID && 
						(selected_rule.heuristic_value < rule.heuristic_value || 
	    				(selected_rule.heuristic_value == rule.heuristic_value && selected_rule.p < rule.p))){
					selected_rule = rule;
				}
	    	}
			
			if(selected_rule != init_rule){
				this.filtered_rule_set.put(selected_rule.signature(), selected_rule);
			}
		}
		
		// Just for testing
		StringBuilder sb = new StringBuilder(100);
		sb.append('\t').append(this.getClass().getSimpleName()).append(' ')
		.append(id).append(" finished in ").append(System.currentTimeMillis()-start).append(" ms");
		System.out.println(sb.toString());
	}

}
