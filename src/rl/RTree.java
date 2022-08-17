/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * RTree for organizing a large number of rules, i.e. association rules.
 * </br>It helps speed up finding rules covering an input example. 
 *
 */
public class RTree {
	private RNode root;
	
	////////////////////////////////////////////// COMMONS METHODS //////////////////////////////////////////////////

	public RTree() {
		this.root = new RNode();
	}
	
	/**
	 * Free memory
	 */
	public void free(){
		root.children.clear();
		root.children = null;
		root = null;
	}
	
	/**
	 * Insert a quantified rule into the RTree
	 * @param body an array of selector IDs of a rule body
	 * @param rule reference of a rule information record
	 */
	public void insert_rule(int[] body, RuleInfo rule) {
		RNode subNode = this.root;
		RNode newNode = null;
		RNode child = null;
        boolean wasNotMerged;
        
        int selector_id, position, size, mid;
        for(int i = 0; i<body.length; i++){
        	selector_id = body[i];
            if(subNode.children != null){
            	wasNotMerged = true;
            	// Find a child which can be merged with the selector
                // If it is so, set flag 'wasNotMerged' to false
            	size = subNode.children.size();
            	position = 0;
                while (position < size) {
                    mid = (position + size) / 2;
                    child = subNode.children.get(mid);
                    if (child.selectorID == selector_id){
                    	subNode = child;
                        wasNotMerged = false;
                        break;
                    } else if (child.selectorID < selector_id) position = mid + 1;
                    else size = mid;
                }
                
                if (wasNotMerged) {
                	newNode = new RNode(subNode, selector_id);
                	subNode.children.add(position, newNode);
                	subNode = newNode;
                }
            }else{
            	subNode.children = new ArrayList<RNode>();
            	newNode = new RNode(subNode, selector_id);
            	subNode.children.add(newNode);
            	subNode = newNode;
            }
        }
        
        // The last node contains the reference to the rule
        if(subNode.rule == null){
        	subNode.rule = rule;
        }else{
        	subNode.rule = RuleComparator.select_better_rule(subNode.rule, rule);
        }
    }
	
	
	/**
	 * Insert a quantified rule into the RTree
	 * @param body an array of selector IDs of a rule body
	 * @param rule reference of a rule information record
	 */
	public void insert_rule_inverse_order(int[] body, RuleInfo rule) {
		RNode subNode = this.root;
		RNode newNode = null;
		RNode child = null;
        boolean wasNotMerged;
        
        int selector_id, position, size, mid;
        for(int i=body.length-1; i>-1; i--){	// ids in the body are inserted in the inverse order
        	selector_id = body[i];
            if(subNode.children != null){
            	wasNotMerged = true;
            	// Find a child which can be merged with the selector
                // If it is so, set flag 'wasNotMerged' to false
            	size = subNode.children.size();
            	position = 0;
                while (position < size) {
                    mid = (position + size) / 2;
                    child = subNode.children.get(mid);
                    if (child.selectorID == selector_id){
                    	subNode = child;
                        wasNotMerged = false;
                        break;
                    } else if (child.selectorID < selector_id) position = mid + 1;
                    else size = mid;
                }
                
                if (wasNotMerged) {
                	newNode = new RNode(subNode, selector_id);
                	subNode.children.add(position, newNode);
                	subNode = newNode;
                }
            }else{
            	subNode.children = new ArrayList<RNode>();
            	newNode = new RNode(subNode, selector_id);
            	subNode.children.add(newNode);
            	subNode = newNode;
            }
        }
        
        // The last node contains the reference to the rule
        if(subNode.rule == null){
        	subNode.rule = rule;
        }else{
        	subNode.rule = RuleComparator.select_better_rule(subNode.rule, rule);
        }
    }
	
	/**
	 * - Insert rules into the RTree and filler rules based on the principle:
	 * </br>rule1: B1 -> C1,  rule2: B2 -> C2; if B2 is super set of B1, rule2.heuristic_value must be greater than rule1.heuristic_value
	 * </br>- Prerequisite: Shorter rules must be are inserted into the tree before the longer rules
	 * @param body
	 * @param rule
	 */
	public void insert_rule_inverse_order_withfilter(int[] body, RuleInfo rule) {
		RNode subNode = this.root;
		RNode newNode = null;
		RNode child = null;
        boolean wasNotMerged;
        
        int selector_id, position, size, mid;
        for(int i=body.length-1; i>-1; i--){	// ids in the body are inserted in the inverse order
        	selector_id = body[i];
            if(subNode.children != null){
            	wasNotMerged = true;
            	// Find a child which can be merged with the selector
                // If it is so, set flag 'wasNotMerged' to false
            	size = subNode.children.size();
            	position = 0;
                while (position < size) {
                    mid = (position + size) / 2;
                    child = subNode.children.get(mid);
                    if (child.selectorID == selector_id){
                    	subNode = child;
                        wasNotMerged = false;
                        if(child.rule != null && child.rule.heuristic_value >= rule.heuristic_value){
                        	return;
                        }
                        break;
                    } else if (child.selectorID < selector_id) position = mid + 1;
                    else size = mid;
                }
                
                if (wasNotMerged) {
                	newNode = new RNode(subNode, selector_id);
                	subNode.children.add(position, newNode);
                	subNode = newNode;
                }
            }else{
            	subNode.children = new ArrayList<RNode>();
            	newNode = new RNode(subNode, selector_id);
            	subNode.children.add(newNode);
            	subNode = newNode;
            }
        }
        
        // The last node contains the reference to the rule
        subNode.rule = rule;
    }
	
	public List<RuleInfo> get_rule_list(){
		List<RuleInfo> rule_list = new ArrayList<RuleInfo>();
		
		for(RNode node : this.root.children) this.collect_rules(node, rule_list);
		
		return rule_list;
	}
	private void collect_rules(RNode node, List<RuleInfo> rule_list){
		if(node.rule != null) rule_list.add(node.rule);
		if(node.children != null){
			for(RNode child_node : node.children) this.collect_rules(child_node, rule_list);
		}
	}
	
	/**
	 * Find all covering rules in the tree
	 * @param example an array of selector IDs based on attribute values of an example
	 * </br> example = [increasingly sorted prediction selector ids][target selector id]
	 * @return list of the found covering rules
	 */
	public List<RuleInfo> find_covering_rules(int[] example){
		List<RuleInfo> covering_rules = new ArrayList<RuleInfo>();
		
		for(RNode child : root.children){
			// The last id in the input example is target selector id which does not present in the tree.
			this.find_covering_rules_recursive(child, example, example.length-2, covering_rules);
		}
		
		return covering_rules;
	}
	private void find_covering_rules_recursive(RNode node, int[] example, int curr_index, List<RuleInfo> covering_rules){
		int next_index;
		for(next_index=curr_index; next_index > -1; next_index--){
			if(example[next_index] == node.selectorID){
				if(node.rule != null){
					covering_rules.add(node.rule);
				}
				next_index--;
				break;
			}
		}
		
		// If no more children or no more matching to test, stop search with the branch
		if(node.children == null || next_index < 0) return;
		
		for(RNode child : node.children){
			find_covering_rules_recursive(child, example, next_index, covering_rules);
		}
	}
	
	
	/**
	 * Find all covering rules in the tree
	 * @param example an array of selector IDs based on attribute values of an example
	 * </br> example = [increasingly sorted prediction selector ids][target selector id]
	 * @return array of the found covering rules
	 */
	public List<RuleInfo> find_covering_rules(Set<Integer> example){
		List<RuleInfo> covering_rules = new ArrayList<RuleInfo>();
		
		for(RNode child : root.children){
			// The last id in the input example is target selector id which does not present in the tree.
			this.find_covering_rules_recursive(child, example, covering_rules);
		}
		
		return covering_rules;
	}
	private void find_covering_rules_recursive(RNode node, Set<Integer> example, List<RuleInfo> covering_rules){
		if(example.contains(node.selectorID)){
			if(node.rule != null){
				covering_rules.add(node.rule);
			}
			// If no more children, stop search with the branch
			if(node.children == null) return;
			
			for(RNode child : node.children){
				find_covering_rules_recursive(child, example, covering_rules);
			}
		}
	}
	
	
	/**
	 * FOR TESTING - Show the content of a small testing tree
	 */
	public void show_rules(){
		LinkedList<RNode> nodeQueue = new LinkedList<RNode>();
		RNode currentNode;
		
		int rule_count = 0;
		int node_count = root.children.size();
    	nodeQueue.addAll(root.children); 	// Add to the end of the list	
    	while(nodeQueue.size() > 0){
    		currentNode = nodeQueue.removeFirst();
    		if(currentNode == null){
    			System.out.println("------");
    		}else{
    			if(currentNode.rule != null){
    				System.out.println(this.get_rule_body(currentNode));
    				rule_count += 1;
    			}
    			
    			if(currentNode.children != null) {
	    			nodeQueue.add(null);	// Add the delimiter
	    			nodeQueue.addAll(currentNode.children);
	    			node_count = node_count + currentNode.children.size();
	    		}
    		}
    	}

    	System.out.println("-------------------------");
    	System.out.println("Node count: " + node_count);
    	System.out.println("Rule count: " + rule_count);
	}
	
	/**
	 * FOR TESTING - Get rule body corresponding to the node
	 */
	public String get_rule_body(RNode node){
		StringBuilder sb = new StringBuilder();
		List<RNode> nodelist = new ArrayList<RNode>();
		
		nodelist.add(node);
		RNode currentNode = node.parent;
		while(currentNode.parent != null){
			nodelist.add(currentNode);
			currentNode = currentNode.parent;
		}
		
		for(int i=nodelist.size()-1; i>-1; i--) sb.append(nodelist.get(i).selectorID).append(' ');
		sb.setLength(sb.length()-1);
		return sb.toString();
	}
}