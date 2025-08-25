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
import java.util.zip.DataFormatException;

/**
 * Treat all attributes in the data as a nominal ones.
 * </br>So if data with numeric or ordinal attributes needs discretization, use ARFFReader instead
 */
public class CSVReader extends DataReader {
	
	public CSVReader(){
		this.data_format = DATA_FORMATS.CSV;
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
	
	private void _fetch_info(BufferedReader input,
							int target_attr_count,
							double support_threshold,
							boolean internal_dnf) throws DataFormatException, IOException{		
		/**
		 * 1. Parse meta data, get the list of attributes, it treats all attributes as nominal ones
		 */
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
		
		/**
		 * 2. Parse data section, construct the list of ATOM selectors (corresponding distinct values) groups based on attributes
		 */		
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
	    min_sup_count = (int) (row_count*support_threshold);
	    
	    /**
	     * 3. Prepare selector structures
	     */
	    if(internal_dnf){
	    	this.prepare_selectors_cnf();
	    }else{
	    	this.prepare_selectors();
	    }
	}
	
}
