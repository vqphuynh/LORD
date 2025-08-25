/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

public abstract class DataReader {
	public static enum DATA_FORMATS {CSV, ARFF};
	
	protected DATA_FORMATS data_format;
	
	protected static final int POWERSET_MAX_ELEMENT_COUNT = 10;
	
	protected String delimiter = ",";
	
	protected BufferedReader input;
	
	/**
	 * List of attributes
	 */
	protected List<Attribute> attributes = new ArrayList<Attribute>();
	
	/**
	 * List of atom selectors (flatten list)
	 */
	protected List<Selector> atom_selectors;
	
	/**
	 * Without CNF, 'constructing_selectors' includes:
	 * </br>+ FREQUENT ATOM selectors from predict attributes in ascending order of support count
	 * </br>+ FREQUENT ATOM selectors from target attributes in ascending order of support count
	 * </br>
	 * </br>
	 * With CNF, 'constructing_selectors' includes:
	 * </br>+ FREQUENT selectors from predict attributes (selectors of the same attribute are continuous together)
	 * </br>+ FREQUENT ATOM selectors from target attributes in ascending order of support count
	 */
	protected List<Selector> constructing_selectors;
	
	/**
	 * Some scalar values
	 */
	protected int row_count = 0, min_sup_count;
	protected int attr_count, predict_attr_count, target_attr_count, numeric_attr_count=0;
	protected int distinct_value_count;
	protected int selector_count, predict_selector_count, target_selector_count;
	
	public DATA_FORMATS getDataFormat(){
		return this.data_format;
	}
	
	public List<Attribute> getAttributes() {
		return attributes;
	}

	public List<Selector> getAtomSelectors() {
		return atom_selectors;
	}

	public List<Selector> getConstructingSelectors() {
		return constructing_selectors;
	}

	public int getRowCount() {
		return row_count;
	}

	public int getMinSupCount() {
		return min_sup_count;
	}

	public int getAttrCount() {
		return attr_count;
	}

	public int getPredictAttrCount() {
		return predict_attr_count;
	}

	public int getTargetAttrCount() {
		return target_attr_count;
	}

	public int getNumericAttrCount() {
		return numeric_attr_count;
	}

	public int getDistinctValueCount() {
		return distinct_value_count;
	}

	public int getSelectorCount() {
		return selector_count;
	}

	public int getPredictSelectorCount() {
		return predict_selector_count;
	}

	public int getTargetSelectorCount() {
		return target_selector_count;
	}
	
	/**
	 * @param datasource_filename
	 * @param target_attr_count
	 * @param support_threshold
	 * @param internal_dnf whether internal Disjunction Normal Form is supported
	 * @throws DataFormatException, IOException
	 */
	public abstract void fetch_info(String datasource_filename,
									int target_attr_count,
									double support_threshold,
									boolean internal_dnf) throws DataFormatException, IOException;
	
	/**
	 * @param data_stream
	 * @param target_attr_count
	 * @param support_threshold
	 * @param internal_dnf whether internal Disjunction Normal Form is supported
	 * @throws DataFormatException, IOException
	 */
	public abstract void fetch_info(InputStream data_stream,
									int target_attr_count,
									double support_threshold,
									boolean internal_dnf) throws DataFormatException, IOException;
	
	
	/**
	 * Open a data source file. Then using 'next_record()' method to fetch data row in form of String[]
	 * @param data_filename
	 * @throws DataFormatException, IOException
	 */
	public abstract void bind_datasource(String data_filename) throws DataFormatException, IOException;
	
	/**
	 * Open a data source file. Then using 'next_record()' method to fetch data row in form of String[]
	 * @param data_stream
	 * @throws DataFormatException, IOException
	 */
	public abstract void bind_datasource(InputStream data_stream) throws DataFormatException, IOException;
	
	/**
	 * Support two formats: .csv, .arff
	 * @param file_name
	 * @return the corresponding CSVReader or ARFFReader, <b>null</b> if unsupported format
	 */
	public static final DataReader getDataReader(String file_name){
		DATA_FORMATS df = getDataFormat(file_name);
        
		switch(df){
			case ARFF:
				return new ARFFReader();
			case CSV:
				return new CSVReader();
			default:
	    		return null;
		}
	}
	
	protected static DATA_FORMATS getDataFormat(String file_name){
		String ext_name = file_name.substring(file_name.lastIndexOf('.')+1).toUpperCase();
		DATA_FORMATS format = DATA_FORMATS.valueOf(ext_name);
		return format;
	}
	
	/**
	 * Set a new delimiter to parse data section.
	 * </br>Default value is ','
	 * @param delimiter
	 */
	public void set_delimiter(String delimiter){
		this.delimiter = delimiter;
	}
	
	protected void prepare_selectors(){
		/**
	     * 1. Construct the selector list 'constructing_selectors' which includes:
	     * 	+ FREQUENT ATOM selectors from PREDICT attributes in ASCENDING ORDER of support count
	     *  + FREQUENT ATOM selectors from TARGET attributes which are in ASCENDING ORDER of support count
	     */
	    
    	/**
    	 * 1.1. Construct list 'predict_selectors' of ATOM selectors from the PREDICTIVE attributes
    	 *      in ASCENDING ORDER of support count
    	 */
	    List<Selector> predict_selectors = new ArrayList<Selector>();
		for(int i=0; i<this.predict_attr_count; i++){
			Map<String, Selector> attr_atom_selector = this.attributes.get(i).distinct_values;
			for(Selector sel: attr_atom_selector.values()){
				predict_selectors.add(sel);
			}
		}
		Collections.sort(predict_selectors, new IncreaseFreqComparator());
	    
	    /**
	     * 1.2. Construct list 'target_selectors' of ATOM selectors from the TARGET attributes
	     *      in ASCENDING ORDER of support count
	     */
		List<Selector> target_selectors = new ArrayList<Selector>();
		for(int i=this.predict_attr_count; i<this.attr_count; i++){
			Map<String, Selector> attr_atom_selector = this.attributes.get(i).distinct_values;
			for(Selector sel: attr_atom_selector.values()){
				target_selectors.add(sel);
			}
		}
		Collections.sort(target_selectors, new IncreaseFreqComparator());
		
		/**
		 * 1.3. 'constructing_selectors' = FREQUENT selectors in 'predict_selectors' + all selectors in 'target_selectors'
		 * 1.4. 'selectors' = 'predict_selectors' + 'target_selectors'
		 */
		this.atom_selectors = new ArrayList<Selector>(predict_selectors.size()+target_selectors.size());
		this.constructing_selectors = new ArrayList<Selector>(predict_selectors.size()+target_selectors.size());
		
		for(Selector sel : predict_selectors){
			this.atom_selectors.add(sel);
			if(sel.frequency < min_sup_count) continue;
			this.constructing_selectors.add(sel);
		}
		this.predict_selector_count = this.constructing_selectors.size();
		
		for(Selector sel : target_selectors){
			this.atom_selectors.add(sel);
			this.constructing_selectors.add(sel);
		}
		
		this.distinct_value_count = this.atom_selectors.size();
		this.selector_count = this.constructing_selectors.size();
		this.target_selector_count = this.selector_count - this.predict_selector_count;
		
		/**
		 * 2. Assign IDs
		 */
		// Assign the index in 'constructing_selectors' list as the selector ID
		// Infrequent ATOM selectors of attributes will remain INVALID_ID
		for(int i=0; i<selector_count; i++) constructing_selectors.get(i).selectorID = i;
				
		// Assign the index in 'atom_selectors' list as distinctValueID
		for(int i=0; i<distinct_value_count; i++) atom_selectors.get(i).distinctValueID = i;
	}
	
	protected void prepare_selectors_cnf(){
	    /**
	     * 3. Construct the selector list 'constructing_selectors' which includes:
	     * 	+ FREQUENT selectors from PREDICTING attributes (selectors of the same attribute are continuous together)
	     *  + FREQUENT ATOM selectors from TARGET attributes which are in ASCENDING ORDER of support count
	     */
	    
	    /**
	     * 3.1. Construct list 'predict_selectors' of FREQUENT selectors from the PREDICTING attributes
	     */
	    List<Selector> predict_selectors = new ArrayList<Selector>();
	    int[][] powerset_template = PowerSetGenerator.generate_power_set(POWERSET_MAX_ELEMENT_COUNT);
    	
    	// Construct list of selectors (disjunction included) from the PREDICTIVE attributes
    	// The order is at attribute level, and stay the original order of attributes in dataset.
    	// The reason is: for example with attributes A and B
    	// 		A:{a1, a2, a1|a2} B:{b1, b2, b1|b2}
    	// 		Assume the order: a1 (a1|a2) < b1 (b1|b2) < a2 (a1|a2) < b2 (b1|b2)
    	// 		So the order between (a1|a2) and (b1|b2) is disordered
    	// 		However, if attribute level order is applied, the order consistency is remained.
    	// 		a1 (a1|a2), a2 (a1|a2) < b1 (b1|b2), b2 (b1|b2)
	    int predict_distinct_value_count = 0;
		for(int id=0; id<predict_attr_count; id++){
			Selector[] attr_atom_selectors = new Selector[this.attributes.get(id).distinct_values.size()];
			String[] attr_values = new String[attr_atom_selectors.length];
			predict_distinct_value_count += attr_atom_selectors.length;	// used to initialize size of 'atom_selectors'
			
			int k=0;
			for(Selector sel : this.attributes.get(id).distinct_values.values()){
				attr_atom_selectors[k] = sel;
				attr_values[k] = sel.distinctValue;
				k++;
			}
			
			String[] str_selectors = 
					PowerSetGenerator.generate_disjunctive_selectors(powerset_template,
																	this.attributes.get(id).name,
																	attr_values);
			
			// This selector list 'selectors' is used to group selectors into sub groups based on atom selectors
			Selector[] selectors = new Selector[str_selectors.length];
			int atom_selector_index = 0;
			Selector atom_selector = attr_atom_selectors[atom_selector_index];
			
			// Note: the empty selector (at 0, null value)
			for(int index=1; index<str_selectors.length; index++){
				if(atom_selector != null && str_selectors[index].equals(atom_selector.condition)){
					selectors[index] = atom_selector;
					atom_selector_index++;
					if(atom_selector_index < attr_atom_selectors.length){
						atom_selector = attr_atom_selectors[atom_selector_index];
					}else atom_selector = null; // no more atom selectors
					continue;
				}
				// Add the new disjunctive selector
				selectors[index] = new Selector(id, this.attributes.get(id).name, str_selectors[index]);
			}
			
			// Build a sup group of selectors for each atom selector
			Selector[][] sub_groups = PowerSetGenerator.group_selectors_by_values(selectors, attr_atom_selectors.length);
			
			// Update frequency for disjunctive selectors in sub groups (Note: full disjunctive selectors is included)
			for(Selector[] sub_group : sub_groups){
				int count = sub_group[0].frequency;	// the first selector in each sub group is an atom selector
				// If a row supports an atom selector, it also supports all disjunctive selectors in the sub group
				for(int i=1; i<sub_group.length; i++) sub_group[i].frequency += count;
			}
			
			// Add FREQUENT selectors to 'predict_selectors'
			// Note: the empty selector (at 0, null value) and full disjunction selectors (at the last index) is NOT used
			int last_index = selectors.length - 1;
			for(int index=1; index<last_index; index++){
				if(selectors[index].frequency < min_sup_count) continue;
				predict_selectors.add(selectors[index]);
			}
			
			// Build the sub group of selectors for each atom selector
			for(Selector[] sub_group : sub_groups){
				Selector atom_sel = sub_group[0];	// the first selector in each sub group is an atom selector
				atom_sel.selectorList = new ArrayList<Selector>(sub_group.length);
				
				for(Selector selector : sub_group){
					// Just add frequent selectors
					if(selector.frequency < min_sup_count) continue;
					atom_sel.selectorList.add(selector);
				}
				// full disjunction selectors (at the last index) is NOT used
				atom_sel.selectorList.remove(atom_sel.selectorList.size()-1);
			}
		}
		predict_selector_count = predict_selectors.size();
		
		/**
		 * 3.2 Construct list 'target_selectors' of ATOM selectors from TARGET attributes
		 * which are in ASCENDING ORDER of support count
		 */
		List<Selector> target_selectors = new ArrayList<Selector>();
		int target_distinct_value_count = 0;
		for(int i=predict_attr_count; i<attr_count; i++){
			Map<String, Selector> atom_selector_group = this.attributes.get(i).distinct_values;
			target_distinct_value_count += atom_selector_group.size();
			
			for(Selector sel: atom_selector_group.values()){
				// With CNF support, distinctValueID is used in TreeNode instead of selectorID
				// With a distinctValueID --> atom selector --> selectorList --> selectorIDs
				// selectorList of atom selector of target attributes must be also not null
				sel.selectorList = new ArrayList<Selector>(1);
				target_selectors.add(sel);
				sel.selectorList.add(sel);
			}
		}
		Collections.sort(target_selectors, new IncreaseFreqComparator());
		target_selector_count = target_selectors.size();
		
		/**
		 * 3.3 'constructing_selectors' = 'predict_selectors' + 'target_selectors'
		 */
		constructing_selectors = new ArrayList<Selector>(predict_selector_count + target_selector_count);
		constructing_selectors.addAll(predict_selectors);
		constructing_selectors.addAll(target_selectors);
		selector_count = constructing_selectors.size();
		
		
		/**
		 * 4. Construct list 'atom_selectors'
		 */
		distinct_value_count = predict_distinct_value_count + target_distinct_value_count;
		atom_selectors = new ArrayList<Selector>(distinct_value_count);
		for(int id=0; id<attr_count; id++){
			for(Selector atom_sel : this.attributes.get(id).distinct_values.values())
				atom_selectors.add(atom_sel);
		}
		
		
		/**
		 * 5. Assign IDs
		 */
		// Assign the index in 'constructing_selectors' list as the selector ID
		// Infrequent selectors of attributes will remain INVALID_ID
		for(int i=0; i<selector_count; i++) constructing_selectors.get(i).selectorID = i;
				
		// Assign the index in 'atom_selectors' list as distinctValueID
		for(int i=0; i<distinct_value_count; i++) atom_selectors.get(i).distinctValueID = i;
		
		// Assign the first selector ID of the next attribute to each selector
		int nextAttr_firstSelectorID = predict_selector_count;
		int currAttrID = constructing_selectors.get(predict_selector_count-1).attributeID;
		Selector sel;
		for(int i=predict_selector_count-1; i>-1; i--){
			sel = constructing_selectors.get(i);
			if(sel.attributeID == currAttrID){
				sel.nextAttr_firstSelectorID = nextAttr_firstSelectorID;
			}else{
				sel.nextAttr_firstSelectorID = nextAttr_firstSelectorID = i+1;
				currAttrID = sel.attributeID;
			}
		}
	}
	
	/**
	 * Sequentially get the next record from the binded data source.
	 * </br>Require the data source that have already been binded with function <b>bind_datasource</b>
	 * @return string array of values
	 * @throws IOException 
	 */
	public String[] next_record() throws IOException{
		String line = null;
		
		if((line = input.readLine()) != null){
			return line.split(delimiter);
		}else{
			input.close();
			return null;
		}
	}
	
	/**
	 * Convert input record of values to the corresponding record of selectorIDs
	 * </br> This function is used when 'fetch_info' function has been called to get information from a data set(train set),
	 * and disjunction selectors are NOT supported.
	 * @param record record of values read from data set
	 * @return record of ids which is selectorID of atom selectors, the length of returned record can be smaller than that of the input record.
	 */
	public int[] convert_values_to_selectorIDs(String[] value_record, int[] id_buffer){
		int count=0;
		Selector s;
		
		for(int i=0; i<value_record.length; i++){
			s = this.attributes.get(i).getSelector(value_record[i]);
			if(s != null && s.selectorID != Selector.INVALID_ID){
				id_buffer[count] = s.selectorID;
				count++;
			}
		}
		
		int[] id_record = new int[count];
		System.arraycopy(id_buffer, 0, id_record, 0, count);
		
		return id_record;
	}
}
