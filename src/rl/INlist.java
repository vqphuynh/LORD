/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

/**
 * Interface I_Nlist
 */
public interface INlist {
	
	/**
	 * Return the number of nodes
	 * @return
	 */
 	public int size();
 	
 	/**
 	 * Return the current capacity
 	 * @return
 	 */
 	public int capacity();
 	
 	/**
 	 * Fill information of the node at position 'index' to the parameter 'node'
 	 * @param index
 	 * @param node
 	 */
 	public void get(int index, Node node);
 	
 	/**
 	 * Return the sum of support counts of all nodes
 	 * @return 
 	 */
 	public int supportCount();
 	
 	/**
 	 * Reset the support count
 	 */
 	public void resetSC();
 	
 	/**
 	 * Allocate capacity which is the number of nodes
 	 * @param capacity
 	 */
 	public void allocate(int capacity);
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * @param efficient_rate: if the size < capacity*efficient_rate, the shrink will be done.
 	 */
 	public INlist shrink(float efficient_rate);
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * </br> Shrink the capacity to the size.
 	 */
 	public INlist shrink();
 	
 	/**
 	 * Return the string representation of the Nlist, just for testing
 	 */
 	public String toString();
 	
 	
 	/////////////////////////////////////////////////////////////////////////////////////////////////////
 	//Optional methods for 'Nodelist' implementation
 	/////////////////////////////////////////////////////////////////////////////////////////////////////
 	/**
 	 * Add a new node to the end of the Nlist
 	 * @param pre
 	 * @param pos
 	 * @param count
 	 */
 	public void add(int pre, int pos, int count);
 	
 	/**
 	 * Based on the information of parameter 'node', a new node is added to the end of the Nlist
 	 * @param node
 	 */
 	public void add(Node node);
 	
 	/**
 	 * Add the 'count' to the support count of node at the position 'index'
 	 * @param index
 	 * @param count
 	 */
 	public void accSupportCount(int index, int count);
 	
 	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	//Optional methods for 'PPCNodelist' implementation which is just for Nlists of single selectors,
 	//for the demand of incremental update on Nlists of single selectors.
	/////////////////////////////////////////////////////////////////////////////////////////////////////
 	/**
 	 * Add the ppcNode to the end of the Nlist.
 	 * @param ppcNode
 	 */
 	public void add(PPCNode ppcNode);
 	
 	/**
 	 * Insert the ppcNode at the appropriate position in the Nlist based on
 	 * pre-code of the ppcNode.
 	 * @param ppcNode
 	 */
 	public void insert(PPCNode ppcNode);
}
