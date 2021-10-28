/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

/**
 * An array implementation for list of nodes. Each node has three properties: pre-code, pos-code, and support count.
 * </br>The purpose is to reduce memory overhead. 
 */
public class Nodelist {
	private static final float allocate_rate = 1.75f;
	private int[][] ppc;
	private int size;
	private int supportCount = -1;
	
 	public Nodelist(int capacity){
 		this.size = 0;
		// ppc[0] for pre-codes, ppc[1] for pos-codes, ppc[2] for support counts
		this.ppc = new int[3][capacity];
	}
 	
 	public Nodelist(){
 		this.size = 0;
		// ppc[0] for pre-codes, ppc[1] for pos-codes, ppc[2] for support counts
		this.ppc = new int[3][16];
	}
 	
 	/**
 	 * New a Nodelist but not allocate any resource. The purpose is to DELAY the allocation
 	 * @param isEmpty	No matter the value of isEmpty is. No allocation!
 	 */
 	public Nodelist(boolean isEmpty){}
 	
 	public int size(){
 		return this.size;
 	}
 	
 	public int capacity(){
 		return this.ppc[0].length;
 	}
 	
 	/**
 	 * This method is associated with the constructor Nodelist(boolean isEmpty)
 	 * @param capacity
 	 */
 	public void allocate(int capacity){
 		if(this.ppc == null){
 			this.size = 0;
 			// ppc[0] for pre-codes, ppc[1] for pos-codes, ppc[2] for support counts
 			this.ppc = new int[3][capacity];
 		}
 	}
 	
 	/**
 	 * Add a new node to the end of the node list
 	 * @param pre
 	 * @param pos
 	 * @param count
 	 */
 	public void add(int pre, int pos, int count){
 		int current_capacity = this.ppc[0].length;
 		if(this.size == current_capacity){
 			// No spare room for new node, allocate new space
 			current_capacity = (int)(current_capacity*allocate_rate);
 			int[][] new_space = new int[3][current_capacity];
 			// Copy
 			System.arraycopy(this.ppc[0], 0, new_space[0], 0, size);
 			System.arraycopy(this.ppc[1], 0, new_space[1], 0, size);
 			System.arraycopy(this.ppc[2], 0, new_space[2], 0, size);
 			this.ppc = new_space;
 		}
 		// Add new node
 		this.ppc[0][size] = pre;
 		this.ppc[1][size] = pos;
 		this.ppc[2][size] = count;
 		this.size++;
 	}
 	
 	/**
 	 * Based on the information of parameter 'node', a new node is add to the end of the node list
 	 * @param node
 	 */
 	public void add(Node node){
 		int current_capacity = this.ppc[0].length;
 		if(this.size == current_capacity){
 			// No spare room for new node, allocate new space
 			current_capacity = (int)(current_capacity*allocate_rate);
 			int[][] new_space = new int[3][current_capacity];
 			// Copy
 			System.arraycopy(this.ppc[0], 0, new_space[0], 0, size);
 			System.arraycopy(this.ppc[1], 0, new_space[1], 0, size);
 			System.arraycopy(this.ppc[2], 0, new_space[2], 0, size);
 			this.ppc = new_space;
 		}
 		// Add new node
 		this.ppc[0][size] = node.pre;
 		this.ppc[1][size] = node.pos;
 		this.ppc[2][size] = node.count;
 		this.size++;
 	}
 	
 	/**
 	 * Fill information of the node at position 'index' to the parameter 'node'
 	 * @param index
 	 * @param node
 	 */
 	public void get(int index, Node node){
 		node.pre = this.ppc[0][index];
 		node.pos = this.ppc[1][index];
 		node.count = this.ppc[2][index];
 	}
 	
 	/**
 	 * Add the 'supportCount' to the support count of node at the position 'index'
 	 * @param index
 	 * @param supportCount
 	 */
 	public void accSupportCount(int index, int supportCount){
 		this.ppc[2][index] += supportCount;
 	}
 	
 	/**
 	 * Summarize all support counts of nodes then return the sum
 	 * @return 
 	 */
 	public int supportCount(){
 		if(this.supportCount == -1){
 			int supportCount=0;
 	 		if(this.ppc != null) for(int count : this.ppc[2]) supportCount += count;
 	 		return (this.supportCount = supportCount);
 		}
 		return this.supportCount;
 	}
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * </br> Shrink the capacity to the size.
 	 * @param efficient_rate: if the size < capacity*efficient_rate, the shrink will be done.
 	 */
 	public void shrink(float efficient_rate){
 		if(this.size < this.ppc[0].length*efficient_rate){
 			// Too much waste room, shrink
 			int[][] new_space = new int[3][size];
 			// Copy
 			System.arraycopy(this.ppc[0], 0, new_space[0], 0, size);
 			System.arraycopy(this.ppc[1], 0, new_space[1], 0, size);
 			System.arraycopy(this.ppc[2], 0, new_space[2], 0, size);
 			this.ppc = new_space;
 		}
 	}
 	
 	public void shrink(){
		int[][] new_space = new int[3][size];
		// Copy
		System.arraycopy(this.ppc[0], 0, new_space[0], 0, size);
		System.arraycopy(this.ppc[1], 0, new_space[1], 0, size);
		System.arraycopy(this.ppc[2], 0, new_space[2], 0, size);
		this.ppc = new_space;
 	}
}
