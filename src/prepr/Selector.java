/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.util.List;


public class Selector {
	public static final int INVALID_ID = -1;
	
	public int attributeID = INVALID_ID;
	public String attributeName;
	public int frequency = 0;
	
	/**
	 * String presentation of a selection condition.
	 * </br>It can be an atom selector, e.g. (A=a1), or an disjunctive selector, e.g. (A=a1||A=a2)
	 */
	public String condition;
	
	/**
	 * The index of the selector in a list of selectors.
	 */
	public int selectorID = INVALID_ID;
	
	/**
	 * Build a selector
	 * @param attribute_id
	 * @param attribute_name
	 * @param condition
	 */
	public Selector(int attribute_id, String attribute_name, String condition){
		this.attributeID = attribute_id;
		this.attributeName = attribute_name;
		this.condition = condition;
	}
	
	/**
	 * Property of atom selectors
	 * </br>A distinct value of an attribute determined by <b>attributeID</b>
	 */
	public String distinctValue;
	
	/**
	 * Property of atom selectors
	 * </br>ID of <b>distinctValue</b>
	 */
	public int distinctValueID = Selector.INVALID_ID;
	
	/**
	 * Property of atom selectors.
	 * </br>List of all selectors sharing the same <b>distinctValue</b>
	 */
	public List<Selector> selectorList = null;
	
	/**
	 * The property is in use when CNF is supported.
	 * </br>This property is helpful in filtering candidate selector IDs from <b>constructing_selectors</b> list
	 * </br>This selector belong to attribute A1, the next attribute is A2
	 * </br>This value is the first selector ID belonging to attribute A2.
	 * 
	 */
	public int nextAttr_firstSelectorID = Selector.INVALID_ID;
	
	/**
	 * Build an atom selector
	 * @param attribute_id the index of the attribute in the attribute list
	 * @param attribute_name attribute name
	 * @param distinctValue a distinct value of the attribute
	 * @param frequency initialized value of frequency
	 */
	public Selector(int attribute_id, String attribute_name, String distinctValue, int frequency){
		this.attributeID = attribute_id;
		this.attributeName = attribute_name;
		this.distinctValue = distinctValue;
		this.condition = new StringBuilder(100).append('(')
				.append(this.attributeName).append('=')
				.append(this.distinctValue).append(')').toString();
		this.frequency = frequency;
	}
	
	private Selector(int attribute_id, 
					String attribute_name, 
					int frequency, 
					String selector, 
					int selectorID,
					String distinct_value,
					int selectorSubGroupID,
					List<Selector> selectorList){
		
		this.attributeID = attribute_id;
		this.attributeName = attribute_name;
		this.frequency = frequency;
		this.condition = selector;
		this.selectorID = selectorID;
		this.distinctValue = distinct_value;
		this.distinctValueID = selectorSubGroupID;
		this.selectorList = selectorList;
	}

	/**
	* Make a copy of this object
	*/
	public Selector clone(){
		return new Selector(this.attributeID, 
							this.attributeName, 
							this.frequency, 
							this.condition, 
							this.selectorID,
							this.distinctValue,
							this.distinctValueID,
							this.selectorList);
	}
}
