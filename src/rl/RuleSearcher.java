/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prepr.Selector;
import evaluations.HeuristicMetric;

public class RuleSearcher {
	
	/**
     * Remove removed_ID from array array_IDs, return new array
     * @param selector_IDs
     * @param removed_ID
     * @return
     */
    private static int[] get_remain_IDs(int[] selector_IDs, int removed_ID){
    	int[] remain_IDs = new int[selector_IDs.length-1];
    	int index = 0;
    	for(int id : selector_IDs){
    		if(id != removed_ID){
    			remain_IDs[index] = id;
    			index++;
    		}
    	}
    	return remain_IDs;
    }
    
    /**
     * Insert new_id to array_IDs at the right position (ascendent order), return the new array
     * @param array_IDs increasingly sorted array of IDs
     * @param new_id
     * @return
     */
    private static int[] get_sorted_array_IDs(int[] array_IDs, int new_id){
    	int[] new_array = new int[array_IDs.length+1];
    	
    	int index = 0;
    	for(; index<array_IDs.length; index++){
    		if(array_IDs[index] < new_id) new_array[index] = array_IDs[index];
    		else{
    			new_array[index] = new_id;
    			break;
    		}
    	}
    	
    	if(index == array_IDs.length){
    		new_array[array_IDs.length] = new_id;
    	}else{
    		for(; index<array_IDs.length; index++){
        		new_array[index+1] = array_IDs[index];
        	}
    	}
    	
    	return new_array;
    }
    
    /**
     * Calculate Nlist of selector ID set, e.g. abcde, assume selector IDs in the order: a < b < c < d < e.
     * </br> Recursively, it calculate sub sets of selector IDs: abcde, abcd, abce, abc, abd, abe, ab, ac, ad, ae.
     * </br> Some Nlists of the selector ID sets may be already in nlist_db.
     * </br> This function's complexity is O(n*n) which is less efficient than the function <b>calculate_nlist_impr</b> with the complexity O(n).
     * @param nlist_db the data base of calculated Nlist of selector ID sets
     * @param k_selector_IDs
     * @return Nlist of the input selectorID set
     */
    protected static Nodelist calculate_nlist(Map<String, Nodelist> nlist_db, int[] k_selector_IDs){
       	String key = Arrays.toString(k_selector_IDs);
       	
    	Nodelist nodelist = nlist_db.get(key);
    	
    	if (nodelist != null) return nodelist;
    	
    	// calculate Nlist of (k-1)_selector_IDs which shares the first (k-1) IDs of k_selector_IDs
    	int[] sub_selector_IDs = new int[k_selector_IDs.length-1];
    	System.arraycopy(k_selector_IDs, 0, sub_selector_IDs, 0, sub_selector_IDs.length);
    	Nodelist nodelist1 = calculate_nlist(nlist_db, sub_selector_IDs);
    	
    	// calculate Nlist of (k-1)_selector_IDs which shares the first (k-2) IDs and the last ID of k_selector_IDs
    	sub_selector_IDs[sub_selector_IDs.length-1] = k_selector_IDs[k_selector_IDs.length-1];
    	Nodelist nodelist2 = calculate_nlist(nlist_db, sub_selector_IDs);
    	
    	nodelist = Supporter.create_nlist(nodelist1, nodelist2);
    	
    	nlist_db.put(key, nodelist);
    	return nodelist;
    }
    
    /**
     * Calculate Nlist of selector ID set, e.g. abcde, assume selector IDs in the order: a < b < c < d < e.
     * </br> Recursively, it calculate sub sets of selector IDs: abcde, abcd, abc, ab.
     * </br> Some Nlists of the selector ID sets may be already in nlist_db.
     * </br> This function's complexity is O(n) which is more efficient than the function <b>calculate_nlist</b> with the complexity O(n*n).
     * @param nlist_db the data base of calculated Nlist of selector ID sets
     * @param k_selector_IDs nlist_db the data base of calculated Nlist of selector ID sets
     * @return Nlist of the input selectorID set
     */
    protected static Nodelist calculate_nlist_impr(Map<String, Nodelist> nlist_db, int[] k_selector_IDs){
       	String key = Arrays.toString(k_selector_IDs);
       	
    	Nodelist nodelist = nlist_db.get(key);
    	
    	if (nodelist != null) return nodelist;
    	
    	// calculate Nlist of (k-1)_selector_IDs which shares the first (k-1) IDs of k_selector_IDs
    	int[] sub_selector_IDs = new int[k_selector_IDs.length-1];
    	System.arraycopy(k_selector_IDs, 0, sub_selector_IDs, 0, sub_selector_IDs.length);
    	Nodelist nodelist1 = calculate_nlist_impr(nlist_db, sub_selector_IDs);
    	
    	// get Nlist of the last selector ID of k_selector_IDs
    	Nodelist nodelist2 = nlist_db.get(Arrays.toString(new int[]{k_selector_IDs[sub_selector_IDs.length]}));
    	
    	nodelist = Supporter.create_nlist(nodelist1, nodelist2);
    	
    	nlist_db.put(key, nodelist);
    	return nodelist;
    }
	
	@SuppressWarnings("unused")
	private static void print_tracing_rules(List<RuleInfo> rule_tracer){
	  	System.out.println("\n\n===========================================");
	  	System.out.println("Rule trace size: " + rule_tracer.size());
	  	for(RuleInfo rule: rule_tracer){
	  		System.out.println(rule.content());
	  	}
	  	System.out.println("===========================================");   	
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////EXHAUSTIVE SEARCH//////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Search for the global best rule based on brute-force search with an auto-pruning technique:
	 * </br>-P1: False Positive 'n' of the current best rule = 0, its True Positive 'p' can be used as a
	 * min_sup threshold to prune.
	 * @param body_selector_IDs
	 * @param class_IDs
	 * @param constructing_selectors
	 * @param prev_level_selector_nodelist
	 * @param metric
	 * @param arguments
	 * @param init_rule
	 * @return
	 */
	public static RuleInfo search_for_global_best_rule(List<Integer> body_selector_IDs,
														List<Integer> class_IDs,
														List<Selector> constructing_selectors,
														Map<String, Nodelist> prev_level_selector_nodelist,
														HeuristicMetric metric,
														double[] arguments,
														RuleInfo init_rule){
		List<RuleInfo> rule_tracer = new ArrayList<RuleInfo>();
		
		RuleInfo current_best_rule;
		if(init_rule == null){
			current_best_rule = new RuleInfo();
	    	current_best_rule.heuristic_value = -Double.MAX_VALUE;
		}else{
			current_best_rule = new RuleInfo();
			current_best_rule.update_info_from(init_rule);
		}
		
		// Recursively search for the best rule in sub spaces
    	for(int id : body_selector_IDs){
    		recursive_search_for_best_rule(new int[]{id},
											body_selector_IDs,
						    				class_IDs,
						    				constructing_selectors,
						    				prev_level_selector_nodelist,
						    				metric,
						    				arguments,
						    				current_best_rule,
						    				rule_tracer);
    	}
    	
    	//print_tracing_rules(rule_tracer);
		
		return current_best_rule;
	}
	
	/**
	 * Search for the global best rule based on brute-force search with an auto-pruning technique:
	 * </br>-P1: False Positive 'n' of the current best rule = 0, its True Positive 'p' can be used as a
	 * min_sup threshold to prune.
	 * </br>-P2: The found best rules of the previous testing examples are cached for the following testing examples.
	 * If no cached best rule is found for a following testing example, the greedy approach (with reduce phase) is
	 * applied for an init rule 'init_rule' so that the pruning by P1 can be applied sooner in the brute-force search.
	 * </br>-P3: The reason that the rules are not as long as the number of attributes of the data set.
	 * a maximum rule length 'max_rule_len' can be applied to the best rule. max_rule_len = init_rule.length + k, e.g k = 1
	 * @param body_selector_IDs
	 * @param class_IDs
	 * @param constructing_selectors
	 * @param prev_level_selector_nodelist
	 * @param metric
	 * @param arguments
	 * @param init_rule
	 * @param max_rule_len includes the rule head
	 * @return
	 */
	public static RuleInfo search_for_global_best_rule(List<Integer> body_selector_IDs,
														List<Integer> class_IDs,
														List<Selector> constructing_selectors,
														Map<String, Nodelist> prev_level_selector_nodelist,
														HeuristicMetric metric,
														double[] arguments,
														RuleInfo init_rule,
														int max_rule_len){
		List<RuleInfo> rule_tracer = new ArrayList<RuleInfo>();
		
		RuleInfo current_best_rule;
		if(init_rule == null){
			current_best_rule = new RuleInfo();
	    	current_best_rule.heuristic_value = -Double.MAX_VALUE;
		}else{
			current_best_rule = new RuleInfo();
			current_best_rule.update_info_from(init_rule);
		}
		
		// Recursively search for the best rule in sub spaces
    	for(int id : body_selector_IDs){
    		recursive_search_for_best_rule(new int[]{id},
											body_selector_IDs,
						    				class_IDs,
						    				constructing_selectors,
						    				prev_level_selector_nodelist,
						    				metric,
						    				arguments,
						    				current_best_rule,
						    				rule_tracer,
						    				max_rule_len,
						    				1);
    	}
    	
    	//print_tracing_rules(rule_tracer);
		
		return current_best_rule;
	}
	
	/**
	 * @param current_body
	 * @param body_selector_IDs
	 * @param class_IDs
	 * @param constructing_selectors
	 * @param prev_level_selector_nodelist
	 * @param metric
	 * @param arguments
	 * @param current_best_rule
	 * @param rule_tracer
	 */
	private static void recursive_search_for_best_rule(int[] current_body,
												List<Integer> body_selector_IDs,
												List<Integer> class_IDs,
												List<Selector> constructing_selectors,
												Map<String, Nodelist> prev_level_selector_nodelist,
												HeuristicMetric metric,
												double[] arguments,
												RuleInfo current_best_rule,
												List<RuleInfo> rule_tracer){
		int size1 = body_selector_IDs.size();
		int size2 = class_IDs.size();
		List<Integer> next_body_selector_IDs = new ArrayList<Integer>(size1);
		List<Integer> next_class_IDs = new ArrayList<Integer>(size2);
		List<int[]> extended_bodies = new ArrayList<int[]>(size1);
		Map<String, Nodelist> curr_level_selector_nodelist = new HashMap<String, Nodelist>(size1+size2);
		
		int[] other_body = current_body.clone();
		int curr_body_last_index = other_body.length-1;
		
		int[] extended_pattern = new int[current_body.length+1];
		System.arraycopy(current_body, 0, extended_pattern, 0, current_body.length);
		int ext_pattern_last_index = other_body.length;
		
    	Nodelist nodelist1 = prev_level_selector_nodelist.get(Arrays.toString(current_body));
    	Nodelist nodelist2, result_nodelist;
    	
    	/*
    	 * Extend with candidate selector IDs from 'body_selector_IDs' (body only)
    	 */
    	for(int id : body_selector_IDs){
    		if(id <= current_body[curr_body_last_index]) continue;
    		
    		other_body[curr_body_last_index] = id;
    		
    		// if((nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body))) == null) continue;
    		nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body));
    		
    		// Calculate the nodelist and support count for the extended selector set
    		result_nodelist = Supporter.create_nlist(nodelist1, nodelist2);
    		
    		// n = 0 means the current best rule already gets highest heuristic value
    		// at this time, the sup_count of the current best rule is used as a min_sup_count to prune.
    		if(current_best_rule.n == 0 && result_nodelist.supportCount() < current_best_rule.p) continue;
    		
    		// Otherwise
    		extended_pattern[ext_pattern_last_index] = id;
    		extended_bodies.add(extended_pattern.clone());	// be careful, must be a copy
    		next_body_selector_IDs.add(id);
			curr_level_selector_nodelist.put(Arrays.toString(extended_pattern), result_nodelist);
    	}
    	
    	/*
    	 * Extend with candidate selector IDs from 'class_IDs' (finding new rules)
    	 */
    	arguments[0] = nodelist1.supportCount(); 		// n+p
    	for(int class_id : class_IDs){
    		other_body[curr_body_last_index] = class_id;
    		
    		// if((nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body))) == null) continue;
    		nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body));
    		
    		// Calculate the nodelist and support count for the rule
    		result_nodelist = Supporter.create_nlist(nodelist1, nodelist2);
    		arguments[1] = result_nodelist.supportCount();	// p, support count of the rule
    		arguments[2] = arguments[0] - arguments[1];		// n
    		arguments[4] = constructing_selectors.get(class_id).frequency;	// P
    		arguments[5] = arguments[3] - arguments[4];		// N
    		
    		// n = 0 means the current best rule already gets highest heuristic value
    		// at this time, the sup_count of the current best rule is used as a min_sup_count to prune.
    		if(current_best_rule.n == 0 && arguments[1] < current_best_rule.p) continue;
    		
    		double heuristic_value = metric.evaluate(arguments);
    		
    		extended_pattern[ext_pattern_last_index] = class_id;
    		next_class_IDs.add(class_id);
    		curr_level_selector_nodelist.put(Arrays.toString(extended_pattern), result_nodelist);
    		
    		if ((current_best_rule.heuristic_value < heuristic_value) ||
        		(current_best_rule.heuristic_value == heuristic_value && current_best_rule.p < arguments[1])){
    			// Note: rule_tracer is just only for checking, not necessary in benchmark
    			// or when list all best rules (the same heuristic value and rule support count)
//    			RuleInfo new_rule = new RuleInfo(arguments[2], arguments[1], arguments[0], current_body.clone(), class_id, heuristic_value);
//    			rule_tracer.add(new_rule);
//    			current_best_rule.update_info_from(new_rule);
    			current_best_rule.update_info_from(arguments[2], arguments[1], arguments[0], current_body.clone(), class_id, heuristic_value);
        	}
    	}
    	
    	// Checking
    	if(next_class_IDs.size() == 0 || extended_bodies.size() == 0) return;
    	
    	// Recursive extending
    	for(int[] ext_body : extended_bodies){
    		recursive_search_for_best_rule(ext_body,
						    				next_body_selector_IDs,
						    				next_class_IDs,
						    				constructing_selectors,
						    				curr_level_selector_nodelist,
											metric,
											arguments,
											current_best_rule,
											rule_tracer);
    	}
	}
	
	/**
	 * @param current_body
	 * @param body_selector_IDs
	 * @param class_IDs
	 * @param constructing_selectors
	 * @param prev_level_selector_nodelist
	 * @param metric
	 * @param arguments
	 * @param current_best_rule
	 * @param rule_tracer
	 * @param max_rule_len
	 * @param deep
	 */
	private static void recursive_search_for_best_rule(int[] current_body,
														List<Integer> body_selector_IDs,
														List<Integer> class_IDs,
														List<Selector> constructing_selectors,
														Map<String, Nodelist> prev_level_selector_nodelist,
														HeuristicMetric metric,
														double[] arguments,
														RuleInfo current_best_rule,
														List<RuleInfo> rule_tracer,
														int max_rule_len,
														int deep){
		int size1 = body_selector_IDs.size();
		int size2 = class_IDs.size();
		List<Integer> next_body_selector_IDs = new ArrayList<Integer>(size1);
		List<Integer> next_class_IDs = new ArrayList<Integer>(size2);
		List<int[]> extended_bodies = new ArrayList<int[]>(size1);
		Map<String, Nodelist> curr_level_selector_nodelist = new HashMap<String, Nodelist>(size1+size2);
		
		int[] other_body = current_body.clone();
		int curr_body_last_index = other_body.length-1;
		
		int[] extended_pattern = new int[current_body.length+1];
		System.arraycopy(current_body, 0, extended_pattern, 0, current_body.length);
		int ext_pattern_last_index = other_body.length;
		
    	Nodelist nodelist1 = prev_level_selector_nodelist.get(Arrays.toString(current_body));
    	Nodelist nodelist2, result_nodelist;
    	
    	/*
    	 * Extend with candidate selector IDs from 'body_selector_IDs' (body only)
    	 */
    	for(int id : body_selector_IDs){
    		if(id <= current_body[curr_body_last_index]) continue;
    		
    		other_body[curr_body_last_index] = id;
    		
    		// if((nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body))) == null) continue;
    		nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body));
    		
    		// Calculate the nodelist and support count for the extended selector set
    		result_nodelist = Supporter.create_nlist(nodelist1, nodelist2);
    		
    		// n = 0 means the current best rule already gets highest heuristic value
    		// at this time, the sup_count of the current best rule is used as a min_sup_count to prune.
    		if(current_best_rule.n == 0 && result_nodelist.supportCount() < current_best_rule.p) continue;
    		
    		// Otherwise
    		extended_pattern[ext_pattern_last_index] = id;
    		extended_bodies.add(extended_pattern.clone());	// be careful, must be a copy
    		next_body_selector_IDs.add(id);
			curr_level_selector_nodelist.put(Arrays.toString(extended_pattern), result_nodelist);
    	}
    	
    	/*
    	 * Extend with candidate selector IDs from 'class_IDs' (finding new rules)
    	 */
    	arguments[0] = nodelist1.supportCount(); 		// n+p
    	for(int class_id : class_IDs){
    		other_body[curr_body_last_index] = class_id;
    		
    		// if((nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body))) == null) continue;
    		nodelist2 = prev_level_selector_nodelist.get(Arrays.toString(other_body));
    		
    		// Calculate the nodelist and support count for the rule
    		result_nodelist = Supporter.create_nlist(nodelist1, nodelist2);
    		arguments[1] = result_nodelist.supportCount();	// p, support count of the rule
    		arguments[2] = arguments[0] - arguments[1];		// n
    		arguments[4] = constructing_selectors.get(class_id).frequency;	// P
    		arguments[5] = arguments[3] - arguments[4];		// N
    		
    		// n = 0 means the current best rule already gets highest heuristic value
    		// at this time, the sup_count of the current best rule is used as a min_sup_count to prune.
    		if(current_best_rule.n == 0 && arguments[1] < current_best_rule.p) continue;
    		
    		double heuristic_value = metric.evaluate(arguments);
    		
    		extended_pattern[ext_pattern_last_index] = class_id;
    		next_class_IDs.add(class_id);
    		curr_level_selector_nodelist.put(Arrays.toString(extended_pattern), result_nodelist);
    		
    		if ((current_best_rule.heuristic_value < heuristic_value) ||
        		(current_best_rule.heuristic_value == heuristic_value && current_best_rule.p < arguments[1])){
    			// Note: rule_tracer is just only for checking, not necessary in benchmark
    			// or when list all best rules (the same heuristic value and rule support count)
//    			RuleInfo new_rule = new RuleInfo(arguments[2], arguments[1], arguments[0], current_body.clone(), class_id, heuristic_value);
//    			rule_tracer.add(new_rule);
//    			current_best_rule.update_info_from(new_rule);
    			current_best_rule.update_info_from(arguments[2], arguments[1], arguments[0], current_body.clone(), class_id, heuristic_value);
        	}
    	}
    	
    	deep++;
    	
    	// Checking and pruning
    	if(deep == max_rule_len || next_class_IDs.size() == 0 || extended_bodies.size() == 0) return;
    	
    	// Recursive extending
    	for(int[] ext_body : extended_bodies){
    		recursive_search_for_best_rule(ext_body,
						    				next_body_selector_IDs,
						    				next_class_IDs,
						    				constructing_selectors,
						    				curr_level_selector_nodelist,
											metric,
											arguments,
											current_best_rule,
											rule_tracer,
											max_rule_len,
											deep);
    	}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////GREEDY SEARCH////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static RuleInfo search_for_greedy_best_rule_verbose(Map<String, Nodelist> selector_nodelist_map,
																int[] body_selector_IDs,
																int class_ID,
																HeuristicMetric metric,
																double[] arguments){
		Map<String, Nodelist> nlist_db = new HashMap<String, Nodelist>(selector_nodelist_map);
    	
    	RuleInfo current_best_rule = new RuleInfo();
    	current_best_rule.body = new int[0];
    	current_best_rule.heuristic_value = -Double.MAX_VALUE;
    	IntHolder chosen_ID = new IntHolder(-1);
    	
    	System.out.println("===============================================");
    	System.out.println("Example body: " + Arrays.toString(body_selector_IDs));
    	
    	int[] remain_selector_IDs = body_selector_IDs;
    	
    	// Grow rule
    	while(remain_selector_IDs.length > 0){
    		RuleInfo next_best_rule = get_extended_current_best_rule(current_best_rule,
																	arguments,
																	metric,
																	nlist_db,
																	remain_selector_IDs,
																	class_ID,
																	chosen_ID);
    		if(next_best_rule == null) break;
    		
    		remain_selector_IDs = get_remain_IDs(remain_selector_IDs, chosen_ID.value);
    		current_best_rule = next_best_rule;
    		
    		System.out.println("Grow to: " + current_best_rule.content());
    	}
    	
    	// Prune rule
    	while(current_best_rule.body.length > 1){
    		RuleInfo next_best_rule = get_pruned_current_best_rule(current_best_rule,
																	arguments,
																	metric,
																	nlist_db,
																	class_ID,
																	chosen_ID);
    		if(next_best_rule == null) break;
    		
    		current_best_rule = next_best_rule;
    		
    		System.out.println("Prune to: " + current_best_rule.content());
    	}
    	    	
    	return current_best_rule;
	}
	
	public static RuleInfo search_for_greedy_best_rule(Map<String, Nodelist> selector_nodelist_map,
														int[] body_selector_IDs,
														int class_ID,
														HeuristicMetric metric,
														double[] arguments){
		Map<String, Nodelist> nlist_db = new HashMap<String, Nodelist>(selector_nodelist_map);
    	
    	RuleInfo current_best_rule = new RuleInfo();
    	current_best_rule.body = new int[0];
    	current_best_rule.heuristic_value = -Double.MAX_VALUE;
    	IntHolder chosen_ID = new IntHolder(-1);
    	
    	int[] remain_selector_IDs = body_selector_IDs;
    	
    	// Grow rule
    	while(remain_selector_IDs.length > 0){
    		RuleInfo next_best_rule = get_extended_current_best_rule(current_best_rule,
																	arguments,
																	metric,
																	nlist_db,
																	remain_selector_IDs,
																	class_ID,
																	chosen_ID);
    		if(next_best_rule == null) break;
    		
    		remain_selector_IDs = get_remain_IDs(remain_selector_IDs, chosen_ID.value);
    		current_best_rule = next_best_rule;
    	}
    	
    	// Prune rule
    	while(current_best_rule.body.length > 1){
    		RuleInfo next_best_rule = get_pruned_current_best_rule(current_best_rule,
																	arguments,
																	metric,
																	nlist_db,
																	class_ID,
																	chosen_ID);
    		if(next_best_rule == null) break;
    		
    		current_best_rule = next_best_rule;
    	}
    	    	
    	return current_best_rule;
	}

	/**
     * Extend the current best rule with each selector ID of "remain_selector_IDs"
     * @param current_best_rule
     * @param arguments
     * @param metric
     * @param remain_selector_IDs
     * @param class_ID
     * @param chosen_ID output parameter, the best selector ID to extend the current best rule
     * @return the next best rule with the chosen selector ID, 
     * null returned if not found a selector ID so that the heuristic value increases
     */
    private static RuleInfo get_extended_current_best_rule(RuleInfo current_best_rule,
					    									double[] arguments,
					    									HeuristicMetric metric,
					    									Map<String, Nodelist> nlist_db,
					    									int[] remain_selector_IDs,
					    									int class_ID,
					    									IntHolder chosen_ID){
    	RuleInfo next_best_rule = null;
    	int[] current_body = current_best_rule.body;
    	
		for(int id : remain_selector_IDs){
			int[] extended_body = get_sorted_array_IDs(current_body, id);
			int[] extended_rule = new int[extended_body.length+1];
			System.arraycopy(extended_body, 0, extended_rule, 0, extended_body.length);
			extended_rule[extended_body.length] = class_ID;
			
    		arguments[0] = calculate_nlist_impr(nlist_db, extended_body).supportCount(); 	// n+p
    		arguments[1] = calculate_nlist_impr(nlist_db, extended_rule).supportCount();	// p
    		arguments[2] = arguments[0] - arguments[1];		// n
    		
    		double heuristic_value = metric.evaluate(arguments);
    		
    		if (current_best_rule.heuristic_value < heuristic_value || 
    				(current_best_rule.heuristic_value == heuristic_value && current_best_rule.p < arguments[1])){
    			next_best_rule = new RuleInfo(arguments[2], arguments[1], arguments[0], extended_body, class_ID, heuristic_value);
    			current_best_rule = next_best_rule;
    			chosen_ID.value = id;
    		}
		}
    	
		return next_best_rule;
    }
    
    private static RuleInfo get_pruned_current_best_rule(RuleInfo current_best_rule,
													double[] arguments,
													HeuristicMetric metric,
													Map<String, Nodelist> nlist_db,
													int class_ID,
													IntHolder chosen_ID){
		RuleInfo next_best_rule = null;
		int[] body_selector_IDs = current_best_rule.body.clone();
		
		for(int id : body_selector_IDs){
			int[] pruned_body = get_remain_IDs(body_selector_IDs, id);
			int[] pruned_rule = new int[body_selector_IDs.length];
			System.arraycopy(pruned_body, 0, pruned_rule, 0, pruned_body.length);
			pruned_rule[pruned_body.length] = class_ID;
			
			arguments[0] = calculate_nlist_impr(nlist_db, pruned_body).supportCount(); 		// n+p
			arguments[1] = calculate_nlist_impr(nlist_db, pruned_rule).supportCount();		// p
			arguments[2] = arguments[0] - arguments[1];		// n
			
			double heuristic_value = metric.evaluate(arguments);
			
			if (current_best_rule.heuristic_value < heuristic_value || 
					(current_best_rule.heuristic_value == heuristic_value && current_best_rule.p < arguments[1])){
				next_best_rule = new RuleInfo(arguments[2], arguments[1], arguments[0], pruned_body, class_ID, heuristic_value);
				current_best_rule = next_best_rule;
				chosen_ID.value = id;
			}
		}

		return next_best_rule;
    }
    
}
