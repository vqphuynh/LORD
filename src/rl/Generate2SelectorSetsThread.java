/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.List;

import rl.IntHolder;
import rl.Matrix;
import rl.PPCNode;

class Generate2SelectorSetsThread extends Thread{
	private List<PPCNode> child_list;
	private Matrix matrix;
	private IntHolder globalIndex;
	private int id;
	
	public Generate2SelectorSetsThread(List<PPCNode> child_list,
									Matrix matrix,
									IntHolder globalIndex,
									int id){
		this.child_list = child_list;
		this.matrix = matrix;
		this.globalIndex = globalIndex;		
		this.id = id;
		this.setPriority(Thread.MAX_PRIORITY);
	}
	
	// Overwrite the run method
	public void run(){
		long start = System.currentTimeMillis();
		
		PPCNode l1_child;
		int size = child_list.size();
		while (true){
			synchronized(globalIndex){
				if(globalIndex.value >= size) break;
				l1_child = child_list.get(globalIndex.value);
				globalIndex.value++;
			}
			
			for(PPCNode l2_child : l1_child.children) update_recursive_supportcount_of_2selector_sets(l2_child);
		}
		
		// Summary local support for 2selector_sets.
		matrix.summary_by_diagonal_folding();
		
		// Just for testing
		StringBuilder sb = new StringBuilder(100);
		sb.append('\t').append(this.getClass().getSimpleName()).append(' ')
		.append(id).append(" finished in ")
		.append(System.currentTimeMillis()-start).append(" ms");
		System.out.println(sb.toString());
	}
	
	/**
	 * This procedure accumulate the support count for 2-itemsets, the items are in item_Index
	 * @param node
	 */
    private void update_recursive_supportcount_of_2selector_sets(PPCNode node){	
    	PPCNode parentNode = node.parent;
    	
    	while(parentNode.parent != null){	// if parentNode.parent == null, parentNode is the root.
    		// Note: node.itemID means selectorID
    		matrix.add(node.itemID, parentNode.itemID, node.count);
    		parentNode = parentNode.parent;
    	}
    	
    	// If the current node is not a leaf node, traverse all its children
    	for(PPCNode child : node.children) update_recursive_supportcount_of_2selector_sets(child);
    }
}
