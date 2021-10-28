/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.ArrayList;
import java.util.List;

public class RNode {
	RNode parent;
	List<RNode> children = null;
	public int selectorID;
	RuleInfo rule = null;
	
	public RNode(){
		this.parent = null;
		this.children = new ArrayList<RNode>();
		this.selectorID = -1;
	}
	
	public RNode(RNode parent, int selectorID){
		this.parent = parent;
		this.selectorID = selectorID;
	}
}
