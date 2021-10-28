/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rl.IntHolder;
import discretizer.Discretizer;
import discretizer.Discretizer.DISCRETIZER;
import discretizer.FUSINTERDiscretizer;
import discretizer.MDLPDiscretizer;

/**
 * - This class discretizes an input data set which is provided via a train set and a test set.
 * </br>- Discretization is performed on the train set, then the discrete values from the train set
 * is applied to discretize the test set.
 * </br>- Note: only ARFF format is supported, assume that data is with the only class attribute at the end
 */
public class DataDiscretizer{	
	private static final String COMMA_DELIMITER = ",";
	private static final String WHITE_DELIMITER = "\\s+";
	private static final String ATTRIBUTE = "@ATTRIBUTE";
	private static final String DATA = "@DATA";
	private static final String NUMERIC = "NUMERIC";
	
	public String delimiter = ",";
	private int numeric_attr_count;
	private int attr_count;
	private List<Attribute> attributes;
	private DISCRETIZER discret_method;
	
	public int getNumericAttrCount() {
		return numeric_attr_count;
	}

	public int getAttrCount() {
		return attr_count;
	}
	
	/**
	 * Using FUSINTER method for discretization
	 */
	public DataDiscretizer(){
		this.attributes = new ArrayList<Attribute>();
		this.discret_method = DISCRETIZER.FUSINTER;
	}
	
	public DataDiscretizer(DISCRETIZER discret_method){
		this.attributes = new ArrayList<Attribute>();
		this.discret_method = discret_method;
	}
	
	/**
	 * Learn discretized values for each numeric attribute in the input data set (train set)
	 * </br>This method must be called before discretizing any data sets (share the same data scheme)
	 * @param train_filename
	 * @return running time
	 * @throws IOException
	 */
	public long learn_discrete_values(String train_filename) throws IOException{
		/**
		 * 1. Parse meta data, get the list of attributes
		 */
		BufferedReader input = new BufferedReader(new FileReader(train_filename));
		String line;
		String[] item_list;
		int attr_id=-1;
		this.numeric_attr_count = 0;
		while ((line = input.readLine()) != null) {
			item_list = line.split(WHITE_DELIMITER);
			if(item_list[0].trim().equalsIgnoreCase(ATTRIBUTE)){
				attr_id++;
				String attribute_name = item_list[1].trim();
				
				// Numeric attribute case
				if(item_list[2].trim().equalsIgnoreCase(NUMERIC)){
					this.numeric_attr_count++;
					Attribute attr = new Attribute(attr_id, attribute_name,
													Attribute.DATA_TYPE.NUMERIC);
					this.attributes.add(attr);
					continue;
				}
				
				// Nominal attribute case
				String[] attr_values = item_list[2].substring(item_list[2].indexOf('{')+1, 
															item_list[2].lastIndexOf('}')).split(COMMA_DELIMITER);
				Map<String, Selector> distinct_values = new HashMap<String, Selector>(attr_values.length);
				for(String v : attr_values){
					String value = v.trim();
					distinct_values.put(value, new Selector(attr_id, attribute_name, value, 0));
				}
				Attribute attr = new Attribute(attr_id, attribute_name,
												Attribute.DATA_TYPE.NOMINAL,
												distinct_values);
				this.attributes.add(attr);
				continue;
			}
			
			if(item_list[0].trim().equalsIgnoreCase(DATA)) break;
		}
		this.attr_count = this.attributes.size();
		int last_attr_index = this.attributes.size()-1;
		
		/**
		 * 2. Prepare class value to class ID 'value_to_classID', assume that the target attribute is the last one
		 * It is just used locally for discretizing numeric attributes
		 */
		IntegerArray classId_of_instances = new IntegerArray();
		Map<String, Integer> value_to_classID = new HashMap<String, Integer>();
		int id = 0;
		for(String value : this.attributes.get(last_attr_index).distinct_values.keySet()){
			value_to_classID.put(value, id);
			id++;
		}
		
		/**
		 * 3. Define where to cache all values of each numeric attribute, but nominal attributes
		 */
		DoubleArray[] numeric_attr_values = new DoubleArray[attr_count];
		for(int i=0; i<this.attr_count; i++){
			if(this.attributes.get(i).type == Attribute.DATA_TYPE.NUMERIC){
				numeric_attr_values[i] = new DoubleArray();
			}
		}
		
		/**
		 * 4. Parse data section, count frequency of distinct values of nominal attributes
		 * and cache all values of numeric attributes
		 */
		String[] value_list;
		while ((line = input.readLine()) != null) {
			value_list = line.split(this.delimiter);
	        
			// Ignore a row if it does not contain the expected number of values
			// Do not know a value belong to which attribute if the number of values is not as expected
			// Every null value is represented by a null symbol
			if(value_list.length != this.attr_count) continue;
	        
	        int attr_index=-1;
	        Attribute attr;
	        for(String value : value_list){
	        	attr_index++;	        	
	        	attr = this.attributes.get(attr_index);
	        	
	        	if(attr.type == Attribute.DATA_TYPE.NOMINAL){
	        		if(Attribute.NULL_SYMBOLS.contains(value)) continue;	// ignore null values
	        		attr.distinct_values.get(value).frequency++;
	        	}else{
	        		// numeric attribute case, record all values including null value (represented as NaN)
	        		if(Attribute.NULL_SYMBOLS.contains(value)) numeric_attr_values[attr_index].add(Double.NaN);
	        		else numeric_attr_values[attr_index].add(Double.parseDouble(value));
	        	}
	        }
	        // record classID of the current example
	        classId_of_instances.add(value_to_classID.get(value_list[last_attr_index]));
		}
	    input.close();
	    
	    /**
	     * 5. Discretize all numeric attributes from the inputs
	     * 		DoubleArray[] numeric_attr_values
	     * 		IntegerArray classId_of_instances
	     * The numeric attribute values at index i are corresponding to classID at index i
	     */
	    long discrete_time = this.discretize_numeric_attributes(numeric_attr_values,
													    		value_to_classID.size(),
													    		classId_of_instances.toArray());
	    return discrete_time;
	}
	
	private long discretize_numeric_attributes(DoubleArray[] numeric_attr_values,
												int class_count,
												int[] classId_of_instances){
		long start = System.currentTimeMillis();
		
		// Threads
        IntHolder globalIndex = new IntHolder(0);
		int thread_count = Math.max(1, Runtime.getRuntime().availableProcessors()/2);
		Thread[] threads = new Thread[thread_count];
		
		for(int i=0; i<thread_count; i++){
			Discretizer discretizer;
			switch(this.discret_method){
				case MDLP:
					discretizer = new MDLPDiscretizer(class_count, classId_of_instances);
					break;
				default:
					discretizer = new FUSINTERDiscretizer(class_count, classId_of_instances);
			}
			threads[i] = new DiscretizationThread(discretizer,
													this.attributes,
													numeric_attr_values,
													globalIndex, i);
			
			threads[i].start();
		}
		
		try {
			for(int i=0; i<thread_count; i++) threads[i].join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return System.currentTimeMillis() - start;
	}
	
	/**
	 * Discrete a file in ARFF format, the output file in CSV format
	 * @param input_filename
	 * @param output_filename
	 * @return row count of the output data set
	 * @throws IOException
	 */
	public int discretize(String input_filename, String output_filename) throws IOException{
		BufferedReader input = new BufferedReader(new FileReader(input_filename));
		BufferedWriter output = new BufferedWriter(new FileWriter(output_filename));
		StringBuilder sb = new StringBuilder(1024);
		
		//write header of the output .csv file
		for(Attribute attr : this.attributes) sb.append(attr.name).append(',');
		sb.setLength(sb.length()-1);
		sb.append('\n');
		output.write(sb.toString());
		
		//pass the meta data part
		String line;
		String[] item_list;
		while ((line = input.readLine()) != null) {
			item_list = line.split(WHITE_DELIMITER);			
			if(item_list[0].trim().equalsIgnoreCase(DATA)) break;
		}
		
		//discrete data and write down
		int row_count = 0;
		String[] value_list;
		while ((line = input.readLine()) != null) {
			sb.setLength(0);
			value_list = line.split(this.delimiter);
	        
			// Ignore a row if it does not contain the expected number of values
			// Do not know a value belong to which attribute if the number of values is not as expected
			// Every null value is represented by a null symbol
			if(value_list.length != this.attr_count) continue;
			else row_count++;
	        
	        int attr_index=-1;
	        Attribute attr;
	        for(String value : value_list){
	        	attr_index++;	        	
	        	attr = this.attributes.get(attr_index);
	        	sb.append(attr.getDiscretizedValue(value)).append(',');
	        }
	        
	        sb.setLength(sb.length()-1);
	        sb.append('\n');
			output.write(sb.toString());
		}
		
	    input.close();
	    output.flush();
	    output.close();
	    
	    return row_count;
	}
}
