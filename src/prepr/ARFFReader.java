/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import rl.IntHolder;
import discretizer.Discretizer;
import discretizer.Discretizer.DISCRETIZER;
import discretizer.FUSINTERDiscretizer;
import discretizer.MDLPDiscretizer;

/**
 * Numeric attributes will be discretized automatically by methods: Fusinter or MDLP
 * </br> MDLP takes more time than Fusinter does but it can be parallelized
 */
public class ARFFReader extends DataReader {
	private static final String COMMA_DELIMITER = ",";
	private static final String WHITE_DELIMITER = "\\s+";
	private static final String ATTRIBUTE = "@ATTRIBUTE";
	private static final String DATA = "@DATA";
	private static final String NUMERIC = "NUMERIC";
	
//	private DISCRETIZER discret_method = DISCRETIZER.MDLP;
	private DISCRETIZER discret_method = DISCRETIZER.FUSINTER;
	
	public ARFFReader(){
		this.data_format = DATA_FORMATS.ARFF;
	}
	
	@Override
	public void bind_datasource(String data_filename) throws DataFormatException, IOException {
		if (this.data_format != DataReader.getDataFormat(data_filename))
			throw new DataFormatException("Require ARFF format");
		
		if(this.input != null) this.input.close();
		
	    // Skip meta data part
		this.input = new BufferedReader(new FileReader(data_filename));
    	String line;
	    while ((line = input.readLine()) != null) {
			if(line.trim().toUpperCase().startsWith(DATA)) break;
		}
	}
	
	public void preprocess(String data_filename,
							int target_attr_count,
							double support_threshold,
							boolean internal_dnf) throws DataFormatException, IOException{
		
		if (this.data_format != DataReader.getDataFormat(data_filename))
			throw new DataFormatException("Require ARFF format");
		
		/**
		 * 1. Parse meta data, get the list of attributes
		 */
		BufferedReader input = new BufferedReader(new FileReader(data_filename));
		String line;
		String[] item_list;
		int attr_id=-1;
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
		this.target_attr_count = target_attr_count;
		this.predict_attr_count = this.attr_count - this.target_attr_count;
		int last_attr_index = this.attr_count-1;
		
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
			else this.row_count++;
	        
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
	    
	    // minimum support count, get the floor value
	    this.min_sup_count = (int) (this.row_count*support_threshold);
	    
	    /**
	     * 5. Discretize all numeric attributes from the inputs
	     * 		DoubleArray[] numeric_attr_values
	     * 		IntegerArray classId_of_instances
	     * The numeric attribute values at index i are corresponding to classID at index i
	     */
	    long discretization_time = this.discretize_numeric_attributes(numeric_attr_values,
															    		value_to_classID.size(),
															    		classId_of_instances.toArray());
	    System.out.println("Discretization time: " + discretization_time);
	    
	    /**
	     * 6. Prepare selector structures
	     */
	    if(internal_dnf){
	    	this.prepare_selectors_cnf();
	    }else{
	    	this.prepare_selectors();
	    }
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
}
