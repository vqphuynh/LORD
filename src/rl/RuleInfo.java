/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.List;

import prepr.Selector;


/**
 * Information record of a rule B -> Class
 */
public class RuleInfo {	
	public int id = -1;
	public double p = -1;			// p = support(B -> Class)
	public double n = -1;			// n = support(B -> !Class) = support(B) - p
	public double n_plus_p = -1;	// n + p = support(B), N = support(!Class), P = support(Class)
	
	public int[] body = null;		// an array of selector IDs of a rule body
	public int headID = -1;			// The ID of the selector corresponding to the class label
	
	public double heuristic_value;
	
	public RuleInfo(){
		
	}
	
	public RuleInfo(double p, int[] body, int head_id){
		this.p = p;
		this.body = body;
		this.headID = head_id;
	}
	
	public RuleInfo(double n, double p, double n_plus_p, int[] body, int head_id){
		this.n = n;
		this.p = p;
		this.n_plus_p = n_plus_p;
		this.body = body;
		this.headID = head_id;
	}
	
	public RuleInfo(double n, double p, double n_plus_p, int[] body, int head_id, double heuristic_value){
		this.n = n;
		this.p = p;
		this.n_plus_p = n_plus_p;
		this.body = body;
		this.headID = head_id;
		this.heuristic_value = heuristic_value;
	}
	
	public void update_info_from(RuleInfo other){
		this.n = other.n;
		this.p = other.p;
		this.n_plus_p = other.n_plus_p;
		this.body = other.body;
		this.headID = other.headID;
		this.heuristic_value = other.heuristic_value;
	}
	
	public void update_info_from(double n, double p, double n_plus_p, int[] body, int head_id, double heuristic_value){
		this.n = n;
		this.p = p;
		this.n_plus_p = n + p;
		this.body = body;
		this.headID = head_id;
		this.heuristic_value = heuristic_value;
	}
	
	/**
	 * Return a copied RuleInfo object of the rule
	 */
	public RuleInfo clone(){
		return new RuleInfo(this.n, this.p, this.n_plus_p, this.body, this.headID, this.heuristic_value);
	}
	
	/**
	 * @return String presentation of the body and the head
	 */
	public String signature(){
		StringBuilder sb = new StringBuilder(50);
		for(int sel_id : this.body) sb.append(sel_id).append(',');
		sb.append(this.headID);
		return sb.toString();
	}
	
	/**
	 * @return Selector-ID-based string presentation of the rule with properties
	 */
	public String content(){
		StringBuilder sb = new StringBuilder(200);
		sb.append('[');
		for(int sel_id : body) sb.append(sel_id).append(',');
		sb.setLength(sb.length()-1);
		sb.append("] -> ").append(this.headID)
		.append("\t(p=").append(this.p)
		.append(", n=").append(this.n)
		.append(", heuristic_value=").append(this.heuristic_value).append(")");
		return sb.toString();
	}
	
	/**
	 * @return Selector-condition-based string presentation of the rule with properties
	 */
	public String content(List<Selector> selectors){
		StringBuilder sb = new StringBuilder(200);
		sb.append("IF ");
		for(int sel_id : body) sb.append(selectors.get(sel_id).condition).append(" & ");
		sb.setLength(sb.length()-3);
		sb.append(" THEN ").append(selectors.get(headID).condition)
		.append("\t(p=").append(this.p)
		.append(", n=").append(this.n)
		.append(", heuristic_value=").append(this.heuristic_value).append(")");
		return sb.toString();
	}
	
	/**
	 * @return Selector-condition-based string presentation of the rule
	 */
	public String content_without_properties(List<Selector> selectors){
		StringBuilder sb = new StringBuilder(200);
		sb.append("IF ");
		for(int sel_id : body) sb.append(selectors.get(sel_id).condition).append(" & ");
		sb.setLength(sb.length()-3);
		sb.append(" THEN ").append(selectors.get(headID).condition);
		return sb.toString();
	}
}
