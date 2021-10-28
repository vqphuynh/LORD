/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.util.ArrayList;
import java.util.List;

public class PowerSetGenerator {
	private static int[] POWER2_VALUES = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};
	
	/**
	 * Non recursive and optimized method to generate all sub sets made of integers in the input array
	 * </br>The output can be used as a template to collect a power set from a set of arbitrary elements
	 * @param array of integers
	 * @return a 2D array containing all sub sets, each is represented by an 1D array
	 */
	public static int[][] generate_power_set(int[] array){
		if(array == null || array.length == 0) return null;
		int[][] results = new int[(int)Math.pow(2, array.length)][];
		
		int start_index = 1;	// results[0] == null, the empty set.
		int upper_index = start_index*2;
		
		for(int i=0; i<array.length; i++){	// scan each element in the input array
			int item = array[i];
			results[start_index] = new int[]{item};
			
			int k = 1;
			for(int j=start_index+1; j<upper_index; j++){
				int[] new_set = new int[results[k].length+1];	// one more room for the new item in a new set
				System.arraycopy(results[k], 0, new_set, 0, results[k].length);	// copy an available set to a new set
				new_set[results[k].length] = item;	// the new item added to the end of every available sets
				results[j] = new_set;
				k++;
			}
			start_index = upper_index;
			upper_index = start_index*2;
		}
		
		return results;
	}
	
	/**
	 * The same as function <b>generate_power_set(int[] array)</b>, but input array is generated automatically
	 * </br>E.g. elements_count=3, the input array = [0, 1, 2]
	 * </br>The output can be used as a template to collect a power set from a set of arbitrary elements
	 * @param elements_count
	 * @return
	 */
	public static int[][] generate_power_set(int elements_count){
		if(elements_count < 1) return null;
		
		int[] array = new int[elements_count];
		for(int i=0; i<elements_count; i++) array[i] = i;
		
		return PowerSetGenerator.generate_power_set(array);
	}	

	/**
	 * Group subsets according to elements.
	 * </br> Every subset in a group that is grouped for element E must contain element E
	 * @param powerset
	 * @param element_count the number of element in the original set
	 * @return list of groups
	 */
	public static List<int[][]> group_subsets_by_elements(int[][] powerset, int element_count){
		List<int[][]> grouped_subsets = new ArrayList<int[][]>(element_count);
		int group_size = powerset.length/2;
		
		int[] power2_values;
		if(element_count >= POWER2_VALUES.length)
			POWER2_VALUES = power2_values = get_power2_values(element_count+1);
		else power2_values = POWER2_VALUES;
		
		for(int i=0; i<element_count; i++){
			int[][] group = new int[group_size][];
			grouped_subsets.add(group);
			int count = power2_values[i];
			int jump = power2_values[i+1];
			int index=0;
			for(int j=count; j<powerset.length; j+=jump){
				for(int k=0; k<count; k++){
					group[index] = powerset[j+k];
					index++;
				}
			}
		}
		
		return grouped_subsets;
	}
	
	/**
	 * Generate optimally all possible disjunctive selectors from distinct values of an attribute.
	 * </br>E.g. attribute A with values a1, a2, a3
	 * </br>All possible disjunctive selectors are:
	 * </br> (A=a1), (A=a2), (A=a3), (A=a1|A=a2), (A=a1|A=a3), (A=a2|A=a3), (A=a1|A=a2|A=a3)
	 * @param powerset_template the output from function <b>generate_power_set</b> which is shared by many calls of this function
	 * @param attr_name name of the attribute
	 * @param attr_values array of distinct values of the attribute
	 * @return list of disjunctive selectors, null if powerset_template is smaller than the expected result.
	 */
	public static String[] generate_disjunctive_selectors(int[][] powerset_template,
															String attr_name,
															String[] attr_values){
		int powerset_length = (int)Math.pow(2, attr_values.length);
		
		if(powerset_length > powerset_template.length) return null;
		
		String[] results = new String[powerset_length];
		StringBuilder sb = new StringBuilder(1024);
		
		int[] subset;
		String attr_name_and_equal = attr_name + "=";
		
		for(int i=1; i<powerset_length; i++){
			subset = powerset_template[i];
			sb.setLength(0);
			sb.append('(');
			
			for(int j=0; j<subset.length; j++){
				sb.append(attr_name_and_equal).append(attr_values[subset[j]]).append('|');
			}
			
			sb.setCharAt(sb.length()-1, ')');
			results[i] = sb.toString();
		}
		
		return results;
	}
	
	/**
	 * Similar to function <b>group_subsets_by_elements(int[][] powerset, int element_count)</b>, this function
	 * </br> group all possible disjunctive selectors into groups based on distinct values of the attribute.
	 * </br> Every selector in the group for a value, e.g. a1, must contain value a1.
	 * @param selectors list of selectors, the output by function <b>generate_disjunctive_selectors</b>
	 * @param value_count number of distinct values of the attribute
	 * @return list of groups of selectors
	 */
	public static Object[][] group_selectors_by_values(Object[] selectors, int value_count){
		Object[][] grouped_selectors = new Object[value_count][];
		int group_size = (int) selectors.length/2;
		
		int[] power2_values;
		if(value_count >= POWER2_VALUES.length)
			POWER2_VALUES = power2_values = get_power2_values(value_count+1);
		else power2_values = POWER2_VALUES;
		
		for(int i=0; i<value_count; i++){
			Object[] group = new Object[group_size];
			grouped_selectors[i] = group;
			int count = power2_values[i];
			int jump = power2_values[i+1];
			int index=0;
			for(int j=count; j<selectors.length; j+=jump){
				for(int k=0; k<count; k++){
					group[index] = selectors[j+k];
					index++;
				}
			}
		}
		
		return grouped_selectors;
	}
	
	/**
	 * Similar to function <b>group_subsets_by_elements(int[][] powerset, int element_count)</b>, this function
	 * </br> group all possible disjunctive selectors into groups based on distinct values of the attribute.
	 * </br> Every selector in the group for a value, e.g. a1, must contain value a1.
	 * @param selectors list of selectors, the output by function <b>generate_disjunctive_selectors</b>
	 * @param value_count number of distinct values of the attribute
	 * @return list of groups of selectors
	 */
	public static Selector[][] group_selectors_by_values(Selector[] selectors, int value_count){
		Selector[][] grouped_selectors = new Selector[value_count][];
		int group_size = (int) selectors.length/2;
		
		int[] power2_values;
		if(value_count >= POWER2_VALUES.length)
			POWER2_VALUES = power2_values = get_power2_values(value_count+1);
		else power2_values = POWER2_VALUES;
		
		for(int i=0; i<value_count; i++){
			Selector[] group = new Selector[group_size];
			grouped_selectors[i] = group;
			int count = power2_values[i];
			int jump = power2_values[i+1];
			int index=0;
			for(int j=count; j<selectors.length; j+=jump){
				for(int k=0; k<count; k++){
					group[index] = selectors[j+k];
					index++;
				}
			}
		}
		
		return grouped_selectors;
	}
	
	/**
	 * Return values of power of 2
	 * </br> e.g. size=3, return [1, 2, 4]
	 * @param size
	 * @return
	 */
	public static int[] get_power2_values(int size){
		int[] power2_values = new int[size];
		int power2 = 1;
		power2_values[0] = power2;
		for(int i=1; i<power2_values.length; i++){
			power2 *= 2;
			power2_values[i] = power2;
		}
		
		return power2_values;
	}
}
