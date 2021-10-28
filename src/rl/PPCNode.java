/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.ArrayList;
import java.util.List;

/**
 * PPCNode of PPCTree
 *
 */
public class PPCNode {
	
	public int pre = 0;
	public int pos = 0;
    public int count = 0;
    
    public int itemID = -1;
    public PPCNode parent = null;
    public List<PPCNode> children = null;
    
    /**
     * Build a root tree node (without parent)
     */
    public PPCNode() {
    	itemID = -1;
        children = new ArrayList<PPCNode>();
    }
    
    /**
     * Build a child tree node having a parent
     */
    public PPCNode(int item_id, PPCNode parent, int count) {
        this.itemID = item_id;
        this.parent = parent;
        this.count = count;
        this.children = new ArrayList<PPCNode>();
    }
    
    /**
     * Build a child tree node having a parent
     */
    public PPCNode(PPCNode parent, int pre, int pos, int item_id, int count) {
    	this.parent = parent;
        this.pre = pre;
        this.pos = pos;
        this.itemID = item_id;
        this.count = count;
        this.children = new ArrayList<PPCNode>();
    }
    
    public PPCNode(int item_id, int count) {
        this.itemID = item_id;
        this.count = count;
        this.children = new ArrayList<PPCNode>();
    }
}
