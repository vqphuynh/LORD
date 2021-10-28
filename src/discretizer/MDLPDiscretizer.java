/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package discretizer;

import java.util.ArrayList;
import java.util.List;

import prepr.DoubleArray;

/**
 * An implementation of MDLP discretization method
 *
 */
public class MDLPDiscretizer extends Discretizer{
	private static final int LEFT_ENT = 0;
	private static final int RIGHT_ENT = 1;
	private static final int COMBINED_ENT = 2;
	private static final int ALL_ENT = 3;
	private static final int LEFT_CC = 4;	// CC stands for class count
	private static final int RIGHT_CC = 5;
	private static final int ALL_CC = 6;
	private static final double LOG2 = Math.log(2);
	
	private int class_count;
	private int[] classId_of_instances;
	
	public MDLPDiscretizer(int class_count, int[] classId_of_instances){
		this.class_count = class_count;
		this.classId_of_instances = classId_of_instances;
	}
	
	@Override
	public DoubleArray discretize_attribute(double[] attr_values, int[] inst_indices, int begin, int end) {
		// Sort 'inst_indices' based on the values of the currently processed attribute 'attr_values'
		this.quick_sort(attr_values, inst_indices, begin, end);
		
		return this.discretize_attribute_recursive(attr_values, inst_indices, begin, end);
	}
	
	public DoubleArray discretize_attribute_recursive(double[] attr_values, int[] inst_indices, int begin, int end) {
		List<Integer> all_cd = this.get_class_distribution(inst_indices, begin, end);
		double all_cc = all_cd.size();
		double all_ent = this.calculate_entropy(all_cd, end-begin+1);
		
		if(all_cc==1) return new DoubleArray(0);
		
		List<Integer> candidate_cutpoints = this.get_candidate_cutpoints(attr_values, inst_indices, begin, end);
		if(candidate_cutpoints.size()==0) return new DoubleArray(0);
		
		int best_cutpoint = candidate_cutpoints.get(0);
		double[] best_result = this.calculate_partition_entropy(inst_indices, begin, best_cutpoint, end);
		
		for(int i=1, size=candidate_cutpoints.size(); i<size; i++){
			int cutpoint = candidate_cutpoints.get(i);
			double[] result = this.calculate_partition_entropy(inst_indices, begin, cutpoint, end);
			if(result[COMBINED_ENT] < best_result[COMBINED_ENT]) {
				best_result = result;
				best_cutpoint = cutpoint;
			}
		}
		
		best_result[ALL_ENT] = all_ent;
		best_result[ALL_CC] = all_cc;
		
		if(best_result[COMBINED_ENT]<all_ent && this.satisfy_split_condition(best_result, end-begin+1)){
			DoubleArray left_discrete_values = this.discretize_attribute_recursive(attr_values, inst_indices, begin, best_cutpoint-1);
			
			double discrete_value = (attr_values[inst_indices[best_cutpoint-1]] + attr_values[inst_indices[best_cutpoint]]) / 2.0;
			left_discrete_values.add(discrete_value);
			
			DoubleArray right_discrete_values = this.discretize_attribute_recursive(attr_values, inst_indices, best_cutpoint, end);
			left_discrete_values.addAll(right_discrete_values);
			
			return left_discrete_values;
		}
		
		return new DoubleArray(0);
	}

	private boolean satisfy_split_condition(double[] result, int ex_count){
		double gain = result[ALL_ENT]-result[COMBINED_ENT];
		double incPart = log2(Math.pow(3, result[ALL_CC])-2) - 
				(result[ALL_CC]*result[ALL_ENT] - result[LEFT_CC]*result[LEFT_ENT] - result[RIGHT_CC]*result[RIGHT_ENT]);
		
		if(gain < (log2(ex_count-1)+incPart)/ex_count) return false;
		
		return true;
	}

	private double log2(double value) {
		return Math.log(value)/LOG2;
	}
	
	private double[] calculate_partition_entropy(int[] instance_indices, int begin, int midPoint, int end) {
		double[] results = new double[7];
		
		List<Integer> cd_left = get_class_distribution(instance_indices, begin, midPoint-1);
		List<Integer> cd_right = get_class_distribution(instance_indices, midPoint, end);

		int ex_count_left = midPoint-begin;
		int ex_count_right = end-midPoint+1;

		results[LEFT_ENT] = calculate_entropy(cd_left, ex_count_left);
		results[RIGHT_ENT] = calculate_entropy(cd_right, ex_count_right);
		results[COMBINED_ENT] = (ex_count_left*results[LEFT_ENT] + ex_count_right*results[RIGHT_ENT])/(ex_count_left+ex_count_right);
		results[LEFT_CC] = cd_left.size();
		results[RIGHT_CC] = cd_right.size();
				
		return results;
	}
	
	private double calculate_entropy(List<Integer> class_distribution, int example_count) {
		double entropy = 0;
		
		for(double freq : class_distribution){
			double proportion = freq/example_count;
			entropy += proportion*Math.log(proportion)/LOG2;
		}
		
		return -entropy;
	}
	
	private List<Integer> get_candidate_cutpoints(double[] attr_values, int[] inst_indices, int begin, int end){
		// Do not know the values of the attribute are one of following 2 cases:
		// 1. Real values that almost they are different to each other
		//		--> It's likely that 'classbased_cut_points' has smaller number of candidate cut points
		//		Moreover, according to MDLP discretization method, these are the best candidate cut points
		// 2. Integer values with small number of distinct value
		//		--> It's likely that 'valuebased_cut_points' has smaller number of candidate cut points
		List<Integer> valuebased_cut_points = new ArrayList<Integer>();
		List<Integer> classbased_cut_points = new ArrayList<Integer>();
		
		double curr_value = attr_values[inst_indices[begin]];
		int curr_classId = this.classId_of_instances[inst_indices[begin]];

		for(int i=begin; i<=end; i++){
			double value = attr_values[inst_indices[i]];
			int classId = this.classId_of_instances[inst_indices[i]];
			if(value != curr_value){
				valuebased_cut_points.add(i);
				curr_value = value;
			}
			if(classId != curr_classId){
				classbased_cut_points.add(i);
				curr_classId = classId;
			}
		}
		if(valuebased_cut_points.size() > classbased_cut_points.size()) return classbased_cut_points;
		else return valuebased_cut_points;
	}

	private List<Integer> get_class_distribution(int[] inst_indices, int begin, int end) {
		int[] classId_frequencies = new int[this.class_count];
		for(int i=0; i<this.class_count; i++) classId_frequencies[i]=0;

		for(int i=begin; i<=end; i++) classId_frequencies[this.classId_of_instances[inst_indices[i]]]++;
		
		List<Integer> result = new ArrayList<Integer>();
		for(int freq : classId_frequencies){
			if(freq > 0) result.add(freq);
		}

		return result;
	}
}