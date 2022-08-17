/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;

import java.util.Map;

import rl.INlist;
import rl.IntHolder;
import rl.RuleInfo;
import rl.RuleSearcher;
import evaluations.HeuristicMetric;
import evaluations.HeuristicMetricFactory;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

class SearchRuleThread extends Thread{
	private int[][] selectorID_records;
	private INlist[] selector_nlists;
	private Map<String, INlist> selector_nlist_map;
	private Map<String, RuleInfo> rule_set;
	private METRIC_TYPES metric_type;
	private double arg;
	private IntHolder globalIndex;
	private int id;
	
	public SearchRuleThread(int[][] selectorID_records,
						INlist[] selector_nlists,
						Map<String, INlist> selector_nlist_map,
						Map<String, RuleInfo> rule_set,
						METRIC_TYPES metric_type,
						double arg,
						IntHolder globalIndex,
						int id){
		this.selectorID_records = selectorID_records;
		this.selector_nlists = selector_nlists;
		this.selector_nlist_map = selector_nlist_map;
		this.rule_set = rule_set;
		this.metric_type = metric_type;
		this.arg = arg;
		this.globalIndex = globalIndex;
		this.id = id;
	}
	
	public void run(){
		long start = System.currentTimeMillis();
		int row_count;
		
		HeuristicMetric metric = HeuristicMetricFactory.getInterestMetric(this.metric_type);
		double[] arguments = new double[7];
    	arguments[3] = row_count = this.selectorID_records.length;
    	arguments[6] = this.arg;
    	
    	int[] example;
    	int example_classID;
		RuleInfo greedy_best_rule;
		
		while (true){
			synchronized(globalIndex){
				if(this.globalIndex.value >= row_count) break;
				example = this.selectorID_records[this.globalIndex.value];
				this.globalIndex.value++;
			}
			
			if(example.length < 2) continue;
			
			example_classID = example[example.length-1];
			int[] body_selector_IDs = new int[example.length-1];
			System.arraycopy(example, 0, body_selector_IDs, 0, body_selector_IDs.length);
			
			arguments[4] = this.selector_nlists[example_classID].supportCount();
			arguments[5] = arguments[3] - arguments[4];
			
			greedy_best_rule = RuleSearcher.search_for_greedy_best_rule(this.selector_nlist_map,
																		body_selector_IDs,
																		example_classID,
																		metric,
																		arguments);
			
			this.rule_set.put(greedy_best_rule.signature(), greedy_best_rule);
		}
		
		// Just for testing
		StringBuilder sb = new StringBuilder(100);
		sb.append('\t').append(this.getClass().getSimpleName()).append(' ')
		.append(id).append(" founds ").append(this.rule_set.size()).append(" rules, finished in ")
		.append(System.currentTimeMillis()-start).append(" ms");
		System.out.println(sb.toString());
	}

}
