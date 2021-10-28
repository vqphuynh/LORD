/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package discretizer;

import prepr.DoubleArray;

/**
 * Abstract class Discretizer to discretize numeric data
 *
 */
public abstract class Discretizer {
	public static enum DISCRETIZER {MDLP, FUSINTER};
	
	protected int candidate_count = -1;
	
	abstract public DoubleArray discretize_attribute(double[] attr_values, int[] inst_indices, int begin, int end);
	
	public int candidate_count() {
		return candidate_count;
	}
	
	/**
     * Do quick sort on 'inst_indices' based on the values of the numeric attribute 'attr_values'.
     * @param attr_values determine which numeric attribute to sort its values.
     * @param inst_indices list of indices to sort, input and output parameters.
     * @param begin the first position of the section to sort.
     * @param end the last position of the section to sort.
     */	
	protected void quick_sort(double[] attr_values, int[] inst_indices, int begin, int end) {
		double pivot;
		int temp;
		int i,j;

		i=begin; j=end;
		pivot = attr_values[inst_indices[(i+j)/2]];
		
		do{
			while(attr_values[inst_indices[i]]<pivot) i++;
			while(attr_values[inst_indices[j]]>pivot) j--;
			if(i<=j){
				if(i<j) {
					temp=inst_indices[i];
					inst_indices[i]=inst_indices[j];
					inst_indices[j]=temp;
				}
				i++; j--;
			}
		}while(i<=j);
		
		if(begin<j) quick_sort(attr_values, inst_indices, begin, j);
		if(i<end) quick_sort(attr_values, inst_indices, i, end);
	}

}
