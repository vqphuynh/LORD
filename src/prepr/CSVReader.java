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
 * Treat all attributes in the data as a nominal ones.
 * </br>So if data with numeric or ordinal attributes needs discretization, use ARFFReader instead
 */
public class CSVReader extends DataReader {
	
	public CSVReader(){
		this.data_format = DATA_FORMATS.CSV;
	}
	
	/**
	 * Setting attribute data type for each attribute will let numerical attributes be discretized.
	 * @param datatypes
	 */
	public void set_attribute_datatypes(String[] datatypes){
		this.attribute_datatypes = new Attribute.DATA_TYPE[datatypes.length];
		for(int i=0; i<datatypes.length; i++){
			Attribute.DATA_TYPE type = Attribute.DATA_TYPE.valueOf(datatypes[i]);
			this.attribute_datatypes[i] = type;
			if (type == Attribute.DATA_TYPE.NUMERIC) this.numeric_attr_count++;
		}
	}
	
	@Override
	public void bind_datasource(String data_filename) throws DataFormatException, IOException {
		if (this.data_format != DataReader.getDataFormat(data_filename))
			throw new DataFormatException("Require CSV format");
		
		if(this.input != null) this.input.close();
		this.input = new BufferedReader(new FileReader(data_filename));
		
	    // Skip attribute name row
    	input.readLine();
	}
	
	@Override
	public void bind_datasource(InputStream data_stream) throws DataFormatException, IOException {    	
    	if(this.input != null) this.input.close();
		this.input = new BufferedReader(new InputStreamReader(data_stream, StandardCharsets.UTF_8));
		
	    // Skip attribute name row
    	input.readLine();
	}
	
	public void fetch_info(String data_filename,
							int target_attr_count,
							double support_threshold,
							boolean internal_dnf) throws DataFormatException, IOException{
		
		if (this.data_format != DataReader.getDataFormat(data_filename))
			throw new DataFormatException("Require CSV format");
		
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
		if (this.attribute_datatypes == null){
			// Parse meta data, treat all attributes as nominal ones
			this.parse_metadata(input, target_attr_count);
			// Parse data, WITHOUT discretization, all attributes are treated as nominal
			this.parse_data(input, target_attr_count, support_threshold, internal_dnf);
		}else{
			// Parse meta data, differentiate numerical and nominal attributes
			this.parse_metadata_support_discretization(input, target_attr_count);
			// Parse data and discretize numeric attributes
			this.parse_data_discretize(input, support_threshold, internal_dnf);
		}
	}
	
	protected void parse_metadata(BufferedReader input, int target_attr_count) throws IOException{
		String line;
		String[] item_list;
		int attr_id=-1;
		if ((line = input.readLine()) != null) {
			item_list = line.split(this.delimiter);
			attr_id++;
			
			for(String attr_name : item_list){
				Attribute attr = new Attribute(attr_id, attr_name, Attribute.DATA_TYPE.NOMINAL,
												new HashMap<String, Selector>());
				this.attributes.add(attr);
			}
		}
		
		this.attr_count = this.attributes.size();
		this.target_attr_count = target_attr_count;
		this.predict_attr_count = this.attr_count - this.target_attr_count;
	}
	
	/**
	 * Parse data section without discretization, all attributes are treated as nominal features
	 * @param input
	 * @param target_attr_count
	 * @param support_threshold
	 * @param internal_dnf
	 * @throws IOException
	 */
	protected void parse_data(BufferedReader input,
							int target_attr_count,
							double support_threshold,
							boolean internal_dnf) throws IOException{
		/**
		 * 1. Parse data section, construct the list of ATOM selectors (corresponding distinct values) groups based on attributes
		 */
		String line;
		String[] item_list;
		while ((line = input.readLine()) != null) {
			item_list = line.split(this.delimiter);
	        
			// Ignore a row if it does not contain the expected number of values
			// Do not know a value belong to which attribute if the number of values is not as expected
			// Every null value is represented by a null symbol
			if(item_list.length != this.attr_count) continue;
	        else this.row_count++;
	        
	        int attr_index = -1;
	        Attribute attr;
	        for(String value : item_list){
	        	attr_index++;
	        	if(Attribute.NULL_SYMBOLS.contains(value)) continue;
	        	
	        	attr = this.attributes.get(attr_index);
	        	Selector s = attr.distinct_values.get(value);
	        	
	        	if(s == null){
	        		attr.distinct_values.put(value, new Selector(attr_index, attr.name, value, 1));
	        	}else{
	        		s.frequency++;
	        	}
	        }
		}
	    input.close();
	    
	    // minimum support count, get the floor value
	    this.min_sup_count = (int) (row_count*support_threshold);
	    
	    /**
	     * 2. Prepare selector structures
	     */
	    if(internal_dnf){
	    	this.prepare_selectors_cnf();
	    }else{
	    	this.prepare_selectors();
	    }
	}
	
	
	protected void parse_metadata_support_discretization(BufferedReader input, int target_attr_count) 
													throws IOException, DataFormatException{
		String line;
		String[] item_list;
		if ((line = input.readLine()) != null) {
			item_list = line.split(this.delimiter);
			
			if (this.attribute_datatypes.length != item_list.length){
				throw new DataFormatException("The numbers of attributes and data types of attributes are not matched!");
			}
			
			int attr_id = 0;
			for(int i=0; i<item_list.length; i++){
				Attribute attr = new Attribute(attr_id, item_list[i], this.attribute_datatypes[i],
												new HashMap<String, Selector>());
				this.attributes.add(attr);
				attr_id ++;
			}
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
		        	Selector s = attr.distinct_values.get(value);
		        	if(s == null){
		        		attr.distinct_values.put(value, new Selector(attr_index, attr.name, value, 1));
		        	}else{
		        		s.frequency++;
		        	}
	        	}else{
	        		// numeric attribute case, record all values including null value (represented as NaN)
	        		if(Attribute.NULL_SYMBOLS.contains(value)) numeric_attr_values[attr_index].add(Double.NaN);
	        		else numeric_attr_values[attr_index].add(Double.parseDouble(value));
	        	}
	        }
	        // record classID of the current example
	        Integer classId = value_to_classID.get(value_list[last_attr_index]);
	        if (classId == null){
	        	classId = value_to_classID.size();
	        	value_to_classID.put(value_list[last_attr_index], classId);
	        }
	        classId_of_instances.add(classId);
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
