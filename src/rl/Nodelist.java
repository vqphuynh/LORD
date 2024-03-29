/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

/**
 * An array implementation for Nlist. Each node has three properties: pre-code, pos-code, and support count.
 * </br>The purpose is to reduce memory overhead.
 */
public class Nodelist implements INlist {
	private static final float allocate_rate = 1.75f;
	private int[][] ppc;
	private int size = 0;
	private int supportCount = -1;
	
 	public Nodelist(int capacity){
		// ppc[0] for pre-codes, ppc[1] for pos-codes, ppc[2] for support counts
		this.ppc = new int[3][capacity];
	}
 	
 	public Nodelist(){
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
 	 * Return the sum of support counts of all nodes
 	 * @return 
 	 */
 	public int supportCount(){
 		if(this.supportCount == -1){
 			int sc = 0;
 	 		if(this.ppc != null) for(int count : this.ppc[2]) sc += count;
 	 		return (this.supportCount = sc);
 		}
 		return this.supportCount;
 	}
 	
 	/**
 	 * Reset the support count
 	 */
 	public void resetSC() {
		this.supportCount = -1;
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
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * @param efficient_rate: if the size < capacity*efficient_rate, the shrink will be done.
 	 */
 	public Nodelist shrink(float efficient_rate){
 		if(this.size < this.ppc[0].length*efficient_rate){
 			// Too much waste room, shrink
 			int[][] new_space = new int[3][size];
 			// Copy
 			System.arraycopy(this.ppc[0], 0, new_space[0], 0, size);
 			System.arraycopy(this.ppc[1], 0, new_space[1], 0, size);
 			System.arraycopy(this.ppc[2], 0, new_space[2], 0, size);
 			this.ppc = new_space;
 		}
 		
 		return this;
 	}
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * </br> Shrink the capacity to the size.
 	 */
 	public Nodelist shrink(){
		int[][] new_space = new int[3][size];
		// Copy
		System.arraycopy(this.ppc[0], 0, new_space[0], 0, size);
		System.arraycopy(this.ppc[1], 0, new_space[1], 0, size);
		System.arraycopy(this.ppc[2], 0, new_space[2], 0, size);
		this.ppc = new_space;
		
		return this;
 	}
 	
 	/**
 	 * Return the string representation of the Nlist, just for testing
 	 */
 	public String toString(){
 		StringBuilder sb = new StringBuilder(200);
 		sb.append('{');
 		for(int i=0; i<size; i++){
 			sb.append('<').append(this.ppc[0][i]).append(',')
 			.append(this.ppc[1][i]).append(">:")
 			.append(this.ppc[2][i]).append("; ");
 		}
 		if (size > 0) sb.setLength(sb.length()-2);	// not an empty list
 		sb.append("} sc:").append(this.supportCount());
 		
 		return sb.toString();
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
 	 * Based on the information of parameter 'node', a new node is added to the end of the node list
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
 	 * Add the 'supportCount' to the support count of node at the position 'index'
 	 * @param index
 	 * @param supportCount
 	 */
 	public void accSupportCount(int index, int supportCount){
 		this.ppc[2][index] += supportCount;
 	}

 	/**
 	 * Nodelist does not support this method.
 	 */
	public void add(PPCNode ppcNode) {
		System.err.println("add(PPCNode ppcNode) method is not supported by Nodelist");
 		System.exit(0);
	}

	/**
	 * Nodelist does not support this method.
	 */
	public void insert(PPCNode ppcNode) {
		System.err.println("insert(PPCNode ppcNode) method is not supported by Nodelist");
 		System.exit(0);
	}
}
