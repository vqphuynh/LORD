/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package discretizer;

import java.util.ArrayList;
import java.util.List;

import prepr.DoubleArray;

/**
 * An implementation of FUSINTER discretization method
 *
 */
public class FUSINTERDiscretizer extends Discretizer{
	private static final double lambda = 1;		// recommended value by the authors of FUSINTER
	private static final double alpha = 0.975;	// recommended value by the authors of FUSINTER
	private static final double one_minus_alpha = 1 - alpha;

	private int class_count;
	private int inst_count;
	private int[] classId_of_instances;
	private double m_x_lambda;	// class count multiplies by lambda
	
	public FUSINTERDiscretizer(int class_count, int[] classId_of_instances){
		this.class_count = class_count;
		this.classId_of_instances = classId_of_instances;
		this.inst_count = classId_of_instances.length;
		this.m_x_lambda = (double)this.class_count * lambda;
	}

	private class Interval {
		public int begin;
		public int end;
		public int[] cd;
		public double impurity;
		public double merge_impurity; // the impurity if this interval is merged with the next one
		public double purity_gain;	// the purity gain if this interval is merged with the next one
		
		public Interval(int[] inst_indices, int begin, int end) {
			this.begin = begin;
			this.end = end;
			this.cd = get_class_distribution(inst_indices, begin, end);
		}
		/**
		 * Calculate phi_2 value of the interval
		 */
		public double calculate_impurity(){
			double Nj = this.end-this.begin+1;	// count of example in the interval
			double sum = 0;
			double Nj_plus_m_x_lambda = Nj + m_x_lambda;
			double factor;
			
			for(int freq : cd) {
				factor = (freq + lambda) / Nj_plus_m_x_lambda;
				sum += factor * (1 - factor);
			}
			
			this.impurity = alpha * (Nj / (double)inst_count) * sum;
			this.impurity += one_minus_alpha * (m_x_lambda / Nj);
			
			return this.impurity;
		}
		
		/**
		 * Calculate phi_2 value of the merged interval of the interval and the next one,
		 * and also calculate the purity gain 
		 * @param next_interval
		 */
		public double calculate_merge_impurity(Interval next_interval){
			double merge_Nj = next_interval.end-this.begin+1;	// count of examples of the merged interval
			
			int[] merge_cd = new int[this.cd.length];
			for(int i=0; i<this.cd.length; i++){
				merge_cd[i] = this.cd[i] + next_interval.cd[i];
			}
			
			double sum = 0;
			double Nj_plus_m_x_lambda = merge_Nj + m_x_lambda;
			double factor;
			
			for(int freq : merge_cd) {
				factor = (freq + lambda) / Nj_plus_m_x_lambda;
				sum += factor * (1 - factor);
			}
			
			this.merge_impurity = alpha * (merge_Nj / (double)inst_count) * sum;
			this.merge_impurity += one_minus_alpha * (m_x_lambda / merge_Nj);
			
			// calculate the purity gain if the interval is merged with the next one
			this.purity_gain = this.impurity + next_interval.impurity - this.merge_impurity;
			
			return this.merge_impurity;
		}

		public void merge(Interval next_interval) {
			this.end = next_interval.end;
			for(int i=0; i<this.cd.length; i++){
				this.cd[i] += next_interval.cd[i];
			}
			this.impurity = this.merge_impurity;
		}
	}
	
	private int[] get_class_distribution(int[] inst_indices, int begin, int end) {
		int[] classId_frequencies = new int[this.class_count];
		for(int i=0; i<this.class_count; i++) classId_frequencies[i]=0;

		for(int i=begin; i<=end; i++) classId_frequencies[this.classId_of_instances[inst_indices[i]]]++;

		return classId_frequencies;	
	}

	public DoubleArray discretize_attribute(double[] attr_values, int[] inst_indices, int begin, int end) {
		// Sort 'inst_indices' based on the values of the currently processed attribute 'attr_values'
		this.quick_sort(attr_values, inst_indices, begin, end);
		
		// Initialize intervals
		List<Interval> intervals = this.initialize_intervals(attr_values, inst_indices, begin, end);
		this.candidate_count = intervals.size();
		
		// Calculate phi_2 value of each interval
		for(Interval interval : intervals) interval.calculate_impurity();
		
		// Calculate phi_2 value of the merged interval of the interval and the next one, and also the purity gain
		for(int i=0, count=intervals.size()-1; i<count; i++){
			intervals.get(i).calculate_merge_impurity(intervals.get(i+1));
		}

		while(intervals.size() > 1) {
			int selected_pos = 0;
			double max_purity_gain = intervals.get(0).purity_gain;
			double purity_gain;
			
			for(int i=0, loop_count=intervals.size()-1; i<loop_count; i++) {
				purity_gain = intervals.get(i).purity_gain;
				
				if(purity_gain > max_purity_gain) {
					selected_pos = i;
					max_purity_gain = purity_gain;
				}
			}

			if(max_purity_gain > 0) {
				int next_pos = selected_pos+1;
				Interval interval = intervals.get(selected_pos);
				
				interval.merge(intervals.get(next_pos));
				intervals.remove(next_pos); // size of intervals now is reduced 1
				
				if(next_pos < intervals.size()) interval.calculate_merge_impurity(intervals.get(next_pos));
				if(selected_pos > 0) intervals.get(selected_pos-1).calculate_merge_impurity(interval);
			} else break;
		}

		// Generate discretized values
		DoubleArray discrete_values = new DoubleArray();
		Interval interval, next_interval;
		for(int i=0, count=intervals.size()-1; i<count; i++) {
			interval = intervals.get(i);
			next_interval = intervals.get(i+1);
			double disct_value = (attr_values[inst_indices[interval.end]] + attr_values[inst_indices[next_interval.begin]])/2.0;
			discrete_values.add(disct_value);
		}
		
		return discrete_values;
	}

	/**
	 * Generate initialized intervals, merge examples sharing the same value or the same class
	 * @param attr_values
	 * @param inst_indices
	 * @param begin
	 * @param end
	 * @return
	 */
	private List<Interval> initialize_intervals(double[] attr_values, int[] inst_indices, int begin, int end) {
		List<Interval> intervals = new ArrayList<Interval>();
		
		int curr_index = begin;
		double curr_value = attr_values[inst_indices[begin]];
		int curr_classId = this.classId_of_instances[inst_indices[begin]];

		for(int i=begin+1; i<=end; i++) {
			double value = attr_values[inst_indices[i]];
			int classId = this.classId_of_instances[inst_indices[i]];
			if(value != curr_value && classId != curr_classId) {
				intervals.add(new Interval(inst_indices, curr_index, i-1));
				curr_index = i;
				curr_value = value;
				curr_classId = classId;
			}
		}
		intervals.add(new Interval(inst_indices, curr_index, end));
		
		return intervals;
	}
}