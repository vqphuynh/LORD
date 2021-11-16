/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Attribute{
	public static final Set<String> NULL_SYMBOLS = new HashSet<String>(Arrays.asList("", "?", " ", "NaN"));
	public static enum DATA_TYPE {NOMINAL,NUMERIC};
	
	/**
	 * The position of the attribute in the list, also the ID of the attribute
	 */
	public int index = -1;
	public String name = null;
	public DATA_TYPE type;
	public Map<String, Selector> distinct_values = null;
	/**
	 * Used only for numeric attributes to determine which distinct interval a value belongs to
	 */
	public double[] discretized_values = null;
	/**
	 * </br>For discrete values: v1 < v2. There are 3 intervals represented as follows
	 * </br> (:v1]		v <= v1
	 * </br> (v1:v2]	v1 < v <= v2
	 * </br> (v2:)		v2 < v
	 */
	private String[] str_intervals = null;
	
	public Attribute(int index, String name, DATA_TYPE type){
		this.index = index;
		this.name = name;
		this.type = type;
	}
	
	public Attribute(int index, String name, DATA_TYPE type, Map<String, Selector> distinct_values){
		this.index = index;
		this.name = name;
		this.type = type;
		this.distinct_values = distinct_values;
	}
	
	/**
	 * Return the corresponding selector of the value
	 * </br>A null can be returned in cases:
	 * </br> 1. Null presentation ('', '?', ' ', 'NaN')
	 * </br> 2. Or a nominal value found in testing data set, but not in training data set
	 * @param value
	 * @return
	 */
	public Selector getSelector(String value){
		switch(this.type){
			case NOMINAL:
				// If a null value ('', ' ', '?', 'NaN') or a value not in training set, a null returned
				return this.distinct_values.get(value);
			case NUMERIC:
				if(Attribute.NULL_SYMBOLS.contains(value)) return null;
				if(this.discretized_values == null){
					// Treat as a nominal attribute because it cannot be discretized.
					// Note: string values of a numeric attribute in 'distinct_values' are now
					// string presentation of double values
					// e.g.  '120' changed to '120.0'
					// Therefore, cannot use directly 'value' as a string to get the corresponding Selector
					return this.distinct_values.get(Double.parseDouble(value)+"");
				}else{
					return this.distinct_values.get(str_intervals[this.find_right_index(
							this.discretized_values, Double.parseDouble(value))]);
				}
			default:
				return null;
		}
	}
	
	public String getDiscretizedValue(String value){
		switch(this.type){
			case NOMINAL:
				return value;	// no matter if value is a null representation
			case NUMERIC:
				if(Attribute.NULL_SYMBOLS.contains(value)) return value;
				if(this.discretized_values == null){
					// Treat as a nominal attribute because it cannot be discretized.
					return value;
				}else{
					return str_intervals[this.find_right_index(this.discretized_values, Double.parseDouble(value))];
				}
			default:
				return value;
		}
	}
	
	public void update_distinct_values_from_discretized_values(double[] discretized_values,
																double[] attr_values){
		if(this.type == DATA_TYPE.NOMINAL) return;
		if(discretized_values.length==0) {
			// If the values of the numeric attribute can not be discretized,
			// Simply treat it as a nominal attribute
			this.distinct_values = this.build_distinct_values_as_nominal_one(attr_values);
			return;
		}
		
		this.discretized_values = discretized_values;
		this.str_intervals = this.build_str_intervals(discretized_values);
		this.distinct_values = this.build_distinct_values(discretized_values, attr_values, this.str_intervals);
	}
	
	private Map<String, Selector> build_distinct_values_as_nominal_one(double[] attr_values){
		Map<String, Selector> distinct_values = new HashMap<String, Selector>();
		
		for(double value : attr_values){
			if(Double.isNaN(value)) continue;
			String str_value = Double.toString(value);
			Selector s = distinct_values.get(str_value);
			if(s==null){
				distinct_values.put(str_value, new Selector(this.index, this.name, str_value, 1));
			}else{
				s.frequency++;
			}
		}
		
		return distinct_values;
	}
	
	private String[] build_str_intervals(double[] discretized_values){
		StringBuilder sb = new StringBuilder(50);
		String[] str_intervals = new String[discretized_values.length+1];
		
		// The first interval
		sb.setLength(0);
		sb.append("(:").append(discretized_values[0]).append(']');
		str_intervals[0] = sb.toString();
		
		for(int i=1; i<discretized_values.length; i++){
			sb.setLength(0);
			sb.append('(').append(discretized_values[i-1])
			.append(':').append(discretized_values[i]).append(']');
			str_intervals[i] = sb.toString();
		}
		
		// The last interval
		sb.setLength(0);
		sb.append('(').append(discretized_values[discretized_values.length-1]).append(":)");
		str_intervals[str_intervals.length-1] = sb.toString();
		
		return str_intervals;
	}
	
	private Map<String, Selector> build_distinct_values(double[] discretized_values,
														double[] attr_values,
														String[] str_intervals){
		Map<String, Selector> distinct_values = new HashMap<String, Selector>(str_intervals.length);
		for(String interval : str_intervals){
			distinct_values.put(interval, new Selector(this.index, this.name, interval, 0));
		}
		
		for(double value : attr_values){
			if(Double.isNaN(value)) continue;
			distinct_values.get(str_intervals[this.find_right_index(discretized_values, value)]).frequency++;
		}
		
		return distinct_values;
	}
	
	private int find_right_index(double[] discretized_values, double value){
		int low_index=0, middle, high_index=discretized_values.length-1;
		
		while((high_index-low_index) > 1){
			middle = (low_index + high_index)/2;
			if(value <= discretized_values[middle]){
				high_index = middle;
			}else{
				low_index = middle;
			}
		}
		if(value <= discretized_values[low_index]) return low_index;
		if(value <= discretized_values[high_index]) return high_index;
		return high_index+1;
	}
}
