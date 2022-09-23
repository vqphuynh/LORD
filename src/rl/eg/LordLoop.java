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
import rl.RuleInfo;
import utilities.MemoryHistogramer;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;


/**
 * Implementation a variant of LORD algorithm, perform iteratively loop of growth and pruning until
 * the currently best rule can not be improved more.
 */
public class LordLoop extends Lord{
	
    public LordLoop(){
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
			
			threads[i] = new SearchRuleThread_LordLoop(this.selectorID_records,
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
		
		// Build the RuleManager
		this.rm = new RuleManager(this.default_classID, ruleSet_list, this.selectorID_records, this.thread_count);
		    	
    	return System.currentTimeMillis()-start;
    }
}
