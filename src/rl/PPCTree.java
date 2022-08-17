/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PPCTree (PrePost Code tree) for generating Nlist of itemset or selector set.
 */
public class PPCTree {
	private PPCNode root;
	private int currentPreCode;
	private int currentPosCode;
	
	////////////////////////////////////////////// COMMONS METHODS //////////////////////////////////////////////////

	public PPCTree() {
		this.root = new PPCNode();
	}
	
	public PPCNode getRoot(){
		return this.root;
	}
	
	/**
	 * Free memory
	 */
	public void free(){
		this.root.children.clear();
		this.root.children = null;
		this.root = null;
	}
	
	/**
     * Store a tree with pre-order traverse
     * </br>One node per line contains the following information: parent_node.pre : node.pre : node.pos : node.itemid : node.support_count
     * @param fileName output file
     * @return running time
     * @throws IOException
     */
    public long storeTree(String fileName) throws IOException{
    	// Remember start time
    	long start = System.currentTimeMillis();
    	
    	BufferedWriter output = new BufferedWriter(new FileWriter(fileName));
    	StringBuilder sb = new StringBuilder();
    	
    	output.write(sb.append(-1).append(':').append(root.pre).append(':').append(root.pos).append(':').
    			append(root.itemID).append(':').append(root.count).append('\n').toString());
    	
    	for(PPCNode child : root.children) storeSubTree(child, output, sb);
    	
    	output.close();
    	
    	// Return time of storing tree
        return System.currentTimeMillis() - start;
    }
    
    /**
     * Store a tree by pre-order traverse, this function will be called recursively
     */
    private static void storeSubTree(PPCNode node, BufferedWriter output, StringBuilder sb) throws IOException{
    	sb.setLength(0);
		output.write(sb.append(node.parent.pre).append(':').append(node.pre).append(':').append(node.pos).append(':').
    			append(node.itemID).append(':').append(node.count).append('\n').toString());
    	
    	for(PPCNode child : node.children) storeSubTree(child, output, sb);
    }
	
	/**
	 * Traverse the tree with pre and post orders and assign two ordinal numbers for each node.
	 * @param root root node of a tree
	 */
	public void assignPrePosOrderCode(){
		this.currentPreCode = 0;
		this.currentPosCode = 0;
		this.traverseAssignPrePosOrderCode(this.root);
	}
    
    /**
     * Traverse the tree with pre&post-order and assign two ordinal numbers for each node
     */
    private void traverseAssignPrePosOrderCode(PPCNode tree_node){
    	// Assign a code for the current node
    	tree_node.pre = currentPreCode;
    	currentPreCode++;
    	
    	// If is not a leaf node, traverse all its children
    	for(PPCNode child : tree_node.children) traverseAssignPrePosOrderCode(child);
    	
    	tree_node.pos = currentPosCode;
		currentPosCode++;
    }
	
	/**
	 * Insert a record of selector ids (in a pre-defined order) into the tree.
	 * </br>The order of ids to insert into the tree is from right to left.
	 * @param record an int array of selector IDs in a pre-defined order of selectors
	 */
	public void insert_record(int[] record){
	    PPCNode new_node, mid_child, sub_node = this.root;
	    boolean wasNotMerged;
	    int id, position, mid_index, size;
	
	    // The record of ids is in ascending order.
	    // So the order of ids to insert into the tree is from right to left.
	    for(int i = record.length-1; i>-1; i--){
	    	id = record[i];
	        wasNotMerged = true;
	        position = 0;
	    	size = sub_node.children.size();
	    	
	    	// Binary search on the id-based ordered children node list of sub_node
	    	while (position < size) {
	    		mid_index = (position + size) / 2;
	            mid_child = sub_node.children.get(mid_index);
	            
	            if (mid_child.itemID < id) position = mid_index + 1;
	            else if (mid_child.itemID > id) size = mid_index;
	            else {
	            	mid_child.count++;
	            	sub_node = mid_child;
	                wasNotMerged = false;
	                break;
	            }
	        }
	        
	        if (wasNotMerged) {
	        	new_node = new PPCNode(id, sub_node, 1);
	        	// position now is the right index in children node list of sub_node
	        	sub_node.children.add(position, new_node);
	        	sub_node = new_node;
	        }
	    }
	}
	
	/**
     * This function will create an Nlist (using Nodelist implementation) for each selector (selector ID) 
     * which was used to build the tree.
     * @param selector_count the number of selectors used to build the tree
     * @return list of Nlists of selectors
     */
     public List<INlist> create_Nlist_for_selectors(int selector_count){    	
    	// Prepare 'selector_nlists', add an empty Nodelist for each selector.
    	// Note: selectorID of a selector is exactly its index in 'selector_nlists' 
    	List<INlist> selector_nlists = new ArrayList<INlist>(selector_count);
    	for(int i=0; i<selector_count; i++){
    		selector_nlists.add(new Nodelist());
    	}
    	
    	// Update selector_nlists
    	for(PPCNode child : this.root.children){
    		this.create_nlists_for_selectors_recursive(child, selector_nlists);
    	}
    	
    	for(INlist nlist : selector_nlists) nlist.shrink();
    	
    	return selector_nlists;
     }
     private void create_nlists_for_selectors_recursive(PPCNode node, List<INlist> selector_nlists){
     	// itemID of a TreeNode means Selector.selectorID
     	selector_nlists.get(node.itemID).add(node.pre, node.pos, node.count);
     	
     	// Recursive call for child nodes
     	for(PPCNode child : node.children) create_nlists_for_selectors_recursive(child, selector_nlists);
     }
	
	/**
     * This function will create an Nlist (using Nodelist implementation) for each selector (selector ID) 
     * which was used to build the tree.
     * @param selector_count the number of selectors used to build the tree
     * @return array of Nlists of selectors
     */
     public INlist[] create_Nlist_for_selectors_arr(int selector_count){
    	// Prepare 'selector_nlists', add an empty Nodelist for each selector.
    	// Note: selectorID of a selector is exactly its index in 'selector_nlists' 
    	INlist[] selector_nlists = new INlist[selector_count];
    	for(int i=0; i<selector_count; i++){
    		selector_nlists[i] = new Nodelist();
    	}
    	
    	// Update selector_nlists
    	for(PPCNode child : this.root.children){
    		this.create_nlists_for_selectors_recursive_arr(child, selector_nlists);
    	}
    	
    	for(INlist nlist : selector_nlists) nlist.shrink();
    	
    	return selector_nlists;
     }
     
     /**
      * This function will create an Nlist (using Nodelist implementation) for each selector (selector ID) 
      * which was used to build the tree.
      * </br>All created Nlists will be add to a map from string representation of each selector ID to the corresponding Nlist.
      * @param total_selector_count the number of selectors used to build the tree
      * @return The map structure from string representation of each selector ID to the corresponding Nlist
      */
      public Map<String, INlist> create_selector_Nlist_map(INlist[] selector_nlists){
    	  int total_selector_count = selector_nlists.length;
    	
    	  // Add all Nlists of selectors to nlistDB
    	  Map<String, INlist> selector_nlist_map = new HashMap<String, INlist>(total_selector_count);
    	  for(int i=0; i<total_selector_count; i++){
    		  selector_nlist_map.put("["+i+"]", selector_nlists[i]);
    	  }
     	
    	  return selector_nlist_map;
      }
	
	/**
     * This function will create an Nlist (using Nodelist implementation) for each selector (selector ID) 
     * which was used to build the tree.
     * </br>All created Nlists will be add to a map from string representation of each selector ID to the corresponding Nlist.
     * @param total_selector_count the number of selectors used to build the tree
     * @return The map structure from string representation of each selector ID to the corresponding Nlist
     */
     public Map<String, INlist> create_selector_Nlist_map(int total_selector_count){
    	// Prepare 'selector_nlists', add an empty Nodelist for each selector.
    	// Note: selectorID of a selector is exactly its index in 'selector_nlists' 
    	INlist[] selector_nlists = new INlist[total_selector_count];
    	for(int i=0; i<total_selector_count; i++){
    		selector_nlists[i] = new Nodelist();
    	}
    	
    	// Update selector_nlists
    	for(PPCNode child : this.root.children){
    		this.create_nlists_for_selectors_recursive_arr(child, selector_nlists);
    	}
    	
    	// Add all Nlists of selectors to nlistDB
    	Map<String, INlist> selector_nlist_map = new HashMap<String, INlist>(total_selector_count);
    	for(int i=0; i<total_selector_count; i++){
    		selector_nlist_map.put("["+i+"]", selector_nlists[i].shrink());
    	}
    	
    	return selector_nlist_map;
    }
    private void create_nlists_for_selectors_recursive_arr(PPCNode node, INlist[] selector_nlists){
    	// itemID of a TreeNode means Selector.selectorID
    	selector_nlists[node.itemID].add(node.pre, node.pos, node.count);
    	
    	// Recursive call for child nodes
    	for(PPCNode child : node.children) create_nlists_for_selectors_recursive_arr(child, selector_nlists);
    }
    
    
    /**
     * In a parallel way, count the support counts, stored in a matrix, of all 2-selector-sets.
     * </br>Column and row indices indicate selector Id, the value at each cell is the corresponding support count of the 2-selector-set
     * @param selector_count
     * @param thread_count
     * @return int matrix of support counts
     * @throws InterruptedException
     */
    public Matrix count_supportcount_of_2selectorSets(int selector_count, int thread_count) throws InterruptedException {    	
    	Matrix[] matrixes = new Matrix[thread_count];
    	for(int i=0; i<thread_count; i++) matrixes[i] = new Matrix(selector_count);
    	Matrix matrix = matrixes[0];
    	IntHolder globalIndex = new IntHolder(0);
    	
    	Thread[] threads = new Thread[thread_count];
    	for(int i=0; i<thread_count; i++){
    		threads[i] = new Generate2SelectorSetsThread(this.root.children, matrixes[i], globalIndex, i);
    		threads[i].start();
        }
        for(int i=0; i<thread_count; i++) threads[i].join();
        
        // Sum all matrixes into the "matrix" (matrixes[0])
        for(int i=1; i<thread_count; i++) matrix.summary_with_matrix(matrixes[i]);
    	
    	return matrix;
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// METHODS Supporting Incremental Learning /////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * This function will create an Nlist (using PPCNodelist implementation) for each selector (selector ID) 
     * which was used to build the tree.
     * @param selector_count the number of selectors used to build the tree
     * @return array of Nlists of selectors
     */
     public INlist[] create_Nlist_for_selectors_arr_inc(int selector_count){
    	// Prepare 'selector_nlists', add an empty PPCNodelist for each selector.
    	// Note: selectorID of a selector is exactly its index in 'selector_nlists' 
    	INlist[] selector_nlists = new INlist[selector_count];
    	for(int i=0; i<selector_count; i++){
    		selector_nlists[i] = new PPCNodelist();
    	}
    	
    	// Update selector_nlists
    	for(PPCNode child : this.root.children){
    		this.create_nlists_for_selectors_recursive_arr_inc(child, selector_nlists);
    	}
    	
    	return selector_nlists;
     }
	 private void create_nlists_for_selectors_recursive_arr_inc(PPCNode node, INlist[] selector_nlists){
	 	// itemID of a TreeNode means Selector.selectorID
	 	selector_nlists[node.itemID].add(node);
	 	
	 	// Recursive call for child nodes
	 	for(PPCNode child : node.children) create_nlists_for_selectors_recursive_arr_inc(child, selector_nlists);
	 }
	 
	 /**
	  * Insert a record of selector ids (in a pre-defined order) into the tree.
	  * </br>The order of ids to insert into the tree is from right to left.
	  * </br>
	  * @param record an int array of selector IDs in a pre-defined order of selectors
	  */
	 
	 /**
	  * Insert a record of selector ids (in a pre-defined order) into the tree.
	  * </br>The order of ids to insert into the tree is from right to left.
	  * @param record an int array of selector IDs in a pre-defined order of selectors
	  * @param selector_nlists Nlists of single selectors
	  * @param new_ppcNodes output parameter, a list of new PPCNodes which have newly inserted into the PPCTree
	  */
	 public void insert_record(int[] record,
		 						INlist[] selector_nlists,
		 						List<PPCNode> new_ppcNodes){
	    PPCNode new_node, mid_child, sub_node = this.root;
	    boolean wasNotMerged;
	    int id, position, mid_index, size;
	
	    // The record of ids is in ascending order.
	    // So the order of ids to insert into the tree is from right to left.
	    for(int i = record.length-1; i>-1; i--){
	    	id = record[i];
	        wasNotMerged = true;
	        position = 0;
	    	size = sub_node.children.size();
	    	
	    	
	    	// Binary search on the id-based ordered children node list of sub_node
	    	while (position < size) {
	    		mid_index = (position + size) / 2;
	            mid_child = sub_node.children.get(mid_index);
	            
	            if (mid_child.itemID < id) position = mid_index + 1;
	            else if (mid_child.itemID > id) size = mid_index;
	            else {
	            	mid_child.count++;
	            	sub_node = mid_child;
	                wasNotMerged = false;
	                break;
	            }
	        }
	        
	        if (wasNotMerged) {
	        	new_node = new PPCNode(id, sub_node, 1);
	        	
	        	// collect the newly added node to insert to the corresponding Nlists
	        	// after reassigning the pre-code and post-code
	        	new_ppcNodes.add(new_node);
	        	
	        	// position now is the right index in children node list of sub_node
	        	sub_node.children.add(position, new_node);
	        	sub_node = new_node;
	        }
	        
	        // reset the support count of the Nlist of the selector with selectorID = 'id'
	        selector_nlists[id].resetSC();
	    }
	}
    
}