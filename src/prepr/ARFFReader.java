/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

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
	
	public ARFFReader(){
		this.data_format = DATA_FORMATS.ARFF;
	}
	
	@Override
	public void set_attribute_datatypes(String[] datatypes) {
		// Do nothing		
	}
	
	@Override
	public void bind_datasource(String data_filename) throws DataFormatException, IOException {
		if (this.data_format != DataReader.getDataFormat(data_filename))
			throw new DataFormatException("Require ARFF format");
		
		if(this.input != null) this.input.close();
		this.input = new BufferedReader(new FileReader(data_filename));
		
	    // Skip meta data part
    	String line;
	    while ((line = input.readLine()) != null) {
			if(line.trim().toUpperCase().startsWith(DATA)) break;
		}
	}
	
	@Override
	public void bind_datasource(InputStream data_stream) throws DataFormatException, IOException {
		if(this.input != null) this.input.close();
		this.input = new BufferedReader(new InputStreamReader(data_stream, StandardCharsets.UTF_8));
		
	    // Skip meta data part
    	String line;
	    while ((line = input.readLine()) != null) {
			if(line.trim().toUpperCase().startsWith(DATA)) break;
		}
	}
	
	public void fetch_info(String data_filename,
							int target_attr_count,
							double support_threshold,
							boolean internal_dnf) throws DataFormatException, IOException{
		
		if (this.data_format != DataReader.getDataFormat(data_filename))
			throw new DataFormatException("Require ARFF format");
		
		BufferedReader input = new BufferedReader(new FileReader(data_filename));
		this._fetch_info(input, target_attr_count, support_threshold, internal_dnf);
	}
	
	@Override
	public void fetch_info(InputStream data_stream, 
							int target_attr_count,
							double support_threshold, 
							boolean internal_dnf) throws DataFormatException, IOException {
		BufferedReader input = new BufferedReader(new InputStreamReader(data_stream, StandardCharsets.UTF_8));
		this._fetch_info(input, target_attr_count, support_threshold, internal_dnf);
	}
	
	protected void _fetch_info(BufferedReader input,
							int target_attr_count,
							double support_threshold,
							boolean internal_dnf) throws DataFormatException, IOException{
		// Parse meta data, get the list of attributes
		this.parse_metadata(input, target_attr_count);
		
		// Parse data and discretize numeric attributes
		this.parse_data_discretize(input, support_threshold, internal_dnf);
	}
	
	protected void parse_metadata(BufferedReader input, int target_attr_count) throws IOException{
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
				String[] attr_values = line.substring(line.indexOf('{')+1, line.lastIndexOf('}')).split(COMMA_DELIMITER);
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
	}
	
	/**
	 * Parse data section and discretizes all numerical attributes
	 * @param input
	 * @param support_threshold
	 * @param internal_dnf
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	protected void parse_data_discretize(BufferedReader input,
										double support_threshold,
										boolean internal_dnf) throws NumberFormatException, IOException{
		/**
		 * 1. Prepare class value to class ID 'value_to_classID', assume that the target attribute is the last one
		 * It is just used locally for discretizing numeric attributes
		 */
		int last_attr_index = this.attr_count-1;
		IntegerArray classId_of_instances = new IntegerArray();
		Map<String, Integer> value_to_classID = new HashMap<String, Integer>();
		int id = 0;
		for(String value : this.attributes.get(last_attr_index).distinct_values.keySet()){
			value_to_classID.put(value, id);
			id++;
		}
		
		/**
		 * 2. Define where to cache all values of each numeric attribute, but nominal attributes
		 */
		DoubleArray[] numeric_attr_values = new DoubleArray[attr_count];
		for(int i=0; i<this.attr_count; i++){
			if(this.attributes.get(i).type == Attribute.DATA_TYPE.NUMERIC){
				numeric_attr_values[i] = new DoubleArray();
			}
		}
		
		/**
		 * 3. Parse data section, count frequency of distinct values of nominal attributes
		 * and cache all values of numeric attributes
		 */
		String line;
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
	     * 4. Discretize all numeric attributes from the inputs
	     * 		DoubleArray[] numeric_attr_values
	     * 		IntegerArray classId_of_instances
	     * The numeric attribute values at index i are corresponding to classID at index i
	     */
	    long discretization_time = this.discretize_numeric_attributes(numeric_attr_values,
															    		value_to_classID.size(),
															    		classId_of_instances.toArray());
	    System.out.println("Discretization time: " + discretization_time);
	    
	    /**
	     * 5. Prepare selector structures
	     */
	    if(internal_dnf){
	    	this.prepare_selectors_cnf();
	    }else{
	    	this.prepare_selectors();
	    }
	}


}
