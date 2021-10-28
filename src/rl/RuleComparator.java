/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.Comparator;

/**
 * Sort decreasingly rules based on heuristic values and then true positive values.
 */
public class RuleComparator implements Comparator<RuleInfo>{

	@Override
	public int compare(RuleInfo rule1, RuleInfo rule2) {
		if(rule2.heuristic_value > rule1.heuristic_value) return 1;
		if(rule2.heuristic_value < rule1.heuristic_value) return -1;
		return (int) (rule2.p - rule1.p);
	}
}
