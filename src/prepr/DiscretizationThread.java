/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.util.List;

import rl.IntHolder;
import discretizer.Discretizer;

public class DiscretizationThread extends Thread{
	private Discretizer discretizer;
	private List<Attribute> attributes;
	private DoubleArray[] numeric_attr_values;
	private IntHolder globalIndex;
	private int id;
	
	public DiscretizationThread(Discretizer discretizer,
								List<Attribute> attributes,
								DoubleArray[] numeric_attr_values,
								IntHolder globalIndex,
								int id){
		this.discretizer = discretizer;
		this.attributes = attributes;
		this.numeric_attr_values = numeric_attr_values;
		this.globalIndex = globalIndex;
		this.id = id;
	}
	
	public void run(){
		System.out.println("Using discretizer: " + discretizer.getClass().getSimpleName());
		StringBuilder sb = new StringBuilder(200);
		int attr_count = this.attributes.size();
		
		Attribute attr;
		int attr_index;
		
		while (true){
			synchronized(globalIndex){
				if(this.globalIndex.value >= attr_count) break;
				attr_index = this.globalIndex.value;
				attr = this.attributes.get(attr_index);
				this.globalIndex.value++;
				if(attr.type == Attribute.DATA_TYPE.NOMINAL) continue;
			}
			
			long inner_start = System.currentTimeMillis();
			
			double[] attr_values = numeric_attr_values[attr_index].toArray();
			
			// Get array of indices where the attribute values are not null
			int[] inst_indices = new int[attr_values.length];
			int not_null_count = 0;
			for(int inst_index=0; inst_index<attr_values.length; inst_index++){
				if(Double.isNaN(attr_values[inst_index])) continue;
				inst_indices[not_null_count] = inst_index;
				not_null_count++;
			}
			
			// Discretize the obtained attribute
			double[] discretized_values = discretizer.discretize_attribute(attr_values, inst_indices, 0, not_null_count-1).toArray();
			attr.update_distinct_values_from_discretized_values(discretized_values, attr_values);
			
			sb.setLength(0);
			sb.append('\t').append(this.getClass().getSimpleName()).append(' ').append(id)
			.append(" discretizes attribute '").append(attr.name)
			.append("', count of candidates: ").append(this.discretizer.candidate_count())
			.append(", count of discretized values: ").append(discretized_values.length)
			.append(", finished in ").append(System.currentTimeMillis()-inner_start).append(" ms");
			System.out.println(sb.toString());
		}
	}
}
