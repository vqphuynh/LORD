/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

/**
 * PPCNodelist class is a PPCNode-based implementation for Nlist of a single selector.
 * </br></br>The purpose is to provide the INCREMENTAL UPDATE for Nlists of single selectors 
 *  when a single or a batch of new training examples is inserted into the current PPCTree.
 *  In the incremental scenario, re-assignment of pre-code and pos-code will be performed 
 *  but fortunately this runs very fast, 
 *  and the re-generation of Nlists of single selectors do not need to be carried. 
 *  Instead, just the newly inserted PPCNodes in the PPCTree will be inserted into the corresponding Nlists.
 *  </br></br>
 *  <b>Note</b>: PPCNodelist is used just for Nlists of single selectors, 
 *  the Nlists of k-selector sets (k>1) still employs Nodelist implementation.
 */
public class PPCNodelist implements INlist {
	private static final float allocate_rate = 1.75f;
	private PPCNode[] ppcNodes;
	private int size = 0;
	private int supportCount = -1;
	
 	public PPCNodelist(int capacity){
		this.ppcNodes = new PPCNode[capacity];
	}
 	
 	public PPCNodelist(){
 		this.ppcNodes = new PPCNode[16];
	}
 	
 	/**
 	 * New a PPCNodelist but not allocate any resource. The purpose is to DELAY the allocation
 	 * @param isEmpty	No matter the value of isEmpty is. No allocation!
 	 */
 	public PPCNodelist(boolean isEmpty){}
 	
 	/**
	 * Return the number of nodes
	 * @return
	 */
 	public int size(){
 		return this.size;
 	}
 	
 	/**
 	 * Return the current capacity
 	 * @return
 	 */
 	public int capacity(){
 		return this.ppcNodes.length;
 	}
 	
 	/**
 	 * Fill information of the node at position 'index' to the parameter 'node'
 	 * @param index
 	 * @param node
 	 */
 	public void get(int index, Node node){
 		PPCNode ppcNode = this.ppcNodes[index];
 		node.pre = ppcNode.pre;
 		node.pos = ppcNode.pos;
 		node.count = ppcNode.count;
 	}
 	
 	/**
 	 * Return the support count
 	 * @return 
 	 */
 	public int supportCount(){
 		if(this.supportCount == -1){
 			int sc = 0;
 			for(int i=0; i<this.size; i++) sc += this.ppcNodes[i].count;
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
 	 * This method is associated with the constructor PPCNodelist(boolean isEmpty)
 	 * @param capacity
 	 */
 	public void allocate(int capacity){
 		if(this.ppcNodes == null){
 			this.size = 0;
 			this.ppcNodes = new PPCNode[capacity];
 		}
 	}
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * @param efficient_rate: if the size < capacity*efficient_rate, the shrink will be done.
 	 */
 	public PPCNodelist shrink(float efficient_rate){
 		if(this.size < this.ppcNodes.length*efficient_rate){
 			// Too much waste room, shrink
 			PPCNode[] new_space = new PPCNode[size];
 			// Copy
 			System.arraycopy(this.ppcNodes, 0, new_space, 0, size);
 			this.ppcNodes = new_space;
 		}
 		
 		return this;
 	}
 	
 	/**
 	 * This function should only be used when being sure that there will not be any new nodes added.
 	 * </br> Shrink the capacity to the size.
 	 */
 	public PPCNodelist shrink(){
 		PPCNode[] new_space = new PPCNode[size];
		// Copy
		System.arraycopy(this.ppcNodes, 0, new_space, 0, size);
		this.ppcNodes = new_space;
		
		return this;
 	}
 	
 	/**
 	 * Return the string representation of the Nlist, just for testing
 	 */
 	public String toString(){
 		PPCNode node;
 		StringBuilder sb = new StringBuilder(200);
 		sb.append('{');
 		for(int i=0; i<size; i++){
 			node = this.ppcNodes[i];
 			sb.append('<')
 			.append(node.pre).append(',')
 			.append(node.pos).append(">:")
 			.append(node.count).append("; ");
 		}
 		if (size > 0) sb.setLength(sb.length()-2);	// not an empty list
 		sb.append("} sc:").append(this.supportCount());
 		
 		return sb.toString();
 	}
 	
 	/**
 	 * PPCNodelist does not support this method
 	 * @param pre
 	 * @param pos
 	 * @param count
 	 * @throws Exception 
 	 */
 	public void add(int pre, int pos, int count){
 		System.err.println("add(int pre, int pos, int count) method is not supported by PPCNodelist");
 		System.exit(0);
 	}
 	
 	/**
 	 * PPCNodelist does not support this method
 	 * @param node
 	 */
 	public void add(Node node){
 		System.err.println("add(Node node) method is not supported by PPCNodelist");
 		System.exit(0);
 	}
 	
 	/**
 	 * PPCNodelist does not support this method
 	 * @param index
 	 * @param supportCount
 	 */
 	public void accSupportCount(int index, int supportCount){
 		System.err.println("accSupportCount(int index, int supportCount) method is not supported by PPCNodelist");
 		System.exit(0);
 	}
 	
 	/**
 	 * Add the ppcNode to the end of the Nlist.
 	 * @param ppcNode
 	 */
 	public void add(PPCNode ppcNode){
 		int current_capacity = this.ppcNodes.length;
 		if(this.size == current_capacity){
 			// No spare room for new node, allocate new space
 			current_capacity = (int)(current_capacity*allocate_rate);
 			PPCNode[] new_space = new PPCNode[current_capacity];
 			// Copy
 			System.arraycopy(this.ppcNodes, 0, new_space, 0, this.size);
 			this.ppcNodes = new_space;
 		}
 		// Add new node
 		this.ppcNodes[this.size] = ppcNode;
 		this.size++;
 	}
 	
 	/**
 	 * Insert the ppcNode at the appropriate position in the Nlist based on
 	 * pre-code of the ppcNode.
 	 * </br>This method is not defined in I_Nlist interface.
 	 * @param ppcNode
 	 */
 	public void insert(PPCNode ppcNode){
 		int position = 0, mid_index, up_index = this.size;
 		PPCNode mid_node;
 		
 		// Binary search for the appropriate index to insert
    	while (position < up_index) {
    		mid_index = (position + up_index) / 2;
    		mid_node = this.ppcNodes[mid_index];
            
            // equal never happens
            if (mid_node.pre < ppcNode.pre) position = mid_index + 1;
            else up_index = mid_index;
        }
    	//System.out.println("position: " + position);
    	
    	int current_capacity = this.ppcNodes.length;
    	if(this.size == current_capacity){
    		// No spare room for new node, allocate new space
 			current_capacity = (int)(current_capacity*allocate_rate);
 			PPCNode[] new_space = new PPCNode[current_capacity];
 			// Copy 
 			// 'position' argument is the size of the first part
 			System.arraycopy(this.ppcNodes, 0, new_space, 0, position);
 			// place ppcNode at the right position
 			new_space[position] = ppcNode;
 			// this.size-'position' argument is the size of the remaining part
 			System.arraycopy(this.ppcNodes, position, new_space, position+1, this.size-position);
 			this.ppcNodes = new_space;
    	}else{
    		System.arraycopy(this.ppcNodes, position, this.ppcNodes, position+1, this.size-position);
    		this.ppcNodes[position] = ppcNode;
    	}
    	
    	this.size++;
 	}
}
