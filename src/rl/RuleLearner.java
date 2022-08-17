/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import prepr.Attribute;
import prepr.DataReader;
import prepr.Selector;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;

/**
 * Abstract class RuleLearner
 *
 */
public abstract class RuleLearner {

	///////////////////////////////////////////////PROPERTIES SECTION//////////////////////////////////////////////	
    protected int thread_count = Math.max(2, Runtime.getRuntime().availableProcessors()/2);
    
    protected String train_filename = null;	// file name of the training dataset
	
    protected int row_count;				// the number of records in the dataset
    protected int min_sup_count;			// minimum support count
    
    protected int attr_count;				// the number of all attributes
    protected int predict_attr_count;		// the number of predict attributes, the first attributes in the attributes list of the dataset
    protected int target_attr_count = 1;	// the number of target attributes, the last attributes in the attributes list of the dataset

	protected int numeric_attr_count;		// the number of numeric attributes
	protected int distinct_value_count;		// the number of distinct values of attributes
	
	protected int selector_count;			// predict_selector_count + target_selector_count
	protected int predict_selector_count; 	// the number of frequent selectors from predict attributes
	protected int target_selector_count;	// the number of selectors from target attributes
	
	
	/**
	 * List of attributes
	 */
	protected List<Attribute> attributes;
	
	/**
	 * List of atom selectors (flatten list)
	 */
	protected List<Selector> atom_selectors;
	
	/**
	 * <b>constructing_selectors</b> includes (in sequence):
	 * </br>1. FREQUENT ATOM selectors from predict attributes
	 * </br>2. FREQUENT ATOM selectors from target attributes which are in ascending order of support count
	 */
	protected List<Selector> constructing_selectors;
	
	/**
	 * Nlists of selectors in <b>constructing_selectors</b>
	 */
	protected Map<String, INlist> selector_nlist_map;
	protected INlist[] selector_nlists;
	protected int[][] selectorID_records;	// training examples in corresponding selector ID sorted in the predefined order O
	
	protected List<Integer> classIDs;	// all class IDs
    protected int default_classID;		// majority classID
    
	
	public List<Attribute> getAttributes(){
		return this.attributes;
	}
	
	public List<Selector> getConstructingSelectors(){
		return this.constructing_selectors;
	}
	
	public int getSelectorCount(){
		return this.selector_count;
	}
	
	public int getTargetSelectorCount(){
		return this.target_selector_count;
	}
	
	public int getPredictSelectorCount(){
		return this.predict_selector_count;
	}
    
    public int getThreadCount(){
    	return this.thread_count;
    }
    
    public List<Integer> getClassIDs(){
    	return this.classIDs;
    }
    
    /**
     * 
     * @param thread_num number of threads to run
     * @param can_exceed_core_num whether the desired number of threads can exceed the number of physical cores.
     */
    public void setThreadCount(int thread_num, boolean can_exceed_core_num){
    	if(can_exceed_core_num){
    		if(thread_num > 0) this.thread_count = thread_num;
    	}else{
    		if(thread_num > 0) this.thread_count = Math.min(this.thread_count, thread_num);
    	}
    }
    
    public int getAttrCount(){
    	return this.attr_count;
    }
    
    public String getTrainFilename(){
    	return this.train_filename;
    }
    
    public int[][] getSelectorIDRecords(){
    	return this.selectorID_records;
    }
	
    ///////////////////////////////////////////////MINING PHASE//////////////////////////////////////////////
    /**
     * RuleLearner constructor
     */
    public RuleLearner(){}
    
    /**
     * Do data preprocessing, build PPCTree and then create Nlist for each distinct selector
     * @return running time of the three stages: [0] preprocessing, [1] build tree, [2] Nlist for each distinct selector
     * @throws IOException
     * @throws DataFormatException 
     */
    public long[] fetch_information(String file_name) throws IOException, DataFormatException {
    	long[] times = new long[3];
    	
        this.train_filename = file_name;
        
        times[0] = this.preprocessing();
        
        PPCTree ppcTree = new PPCTree(); 
        times[1] = this.construct_tree(ppcTree);
        
        long start = System.currentTimeMillis();
        this.selector_nlists = ppcTree.create_Nlist_for_selectors_arr(this.selector_count);
        this.selector_nlist_map = ppcTree.create_selector_Nlist_map(this.selector_nlists);
        RuleSearcher.setSelectorNlists(this.selector_nlists);
        times[2] = System.currentTimeMillis() - start;
        
        return times;
    }
    
    /**
     * @param file_name
     * @return
     * @throws IOException
     * @throws DataFormatException
     */
    public PPCTree fetch_information_return_PPCtree(String file_name) throws IOException, DataFormatException {
    	long[] times = new long[3];
    	
        this.train_filename = file_name;
        
        times[0] = this.preprocessing();
        
        PPCTree ppcTree = new PPCTree(); 
        times[1] = this.construct_tree(ppcTree);
        
        long start = System.currentTimeMillis();
        this.selector_nlists = ppcTree.create_Nlist_for_selectors_arr(this.selector_count);
        this.selector_nlist_map = ppcTree.create_selector_Nlist_map(this.selector_nlists);
        RuleSearcher.setSelectorNlists(this.selector_nlists);
        times[2] = System.currentTimeMillis() - start;
        
        return ppcTree;
    }
    
    public void write_nlists(String file_name) throws IOException{
    	BufferedWriter w = new BufferedWriter(new FileWriter(file_name));
    	for(INlist nlist : this.selector_nlists){
    		w.write(nlist.toString());
    		w.write('\n');
    	}
    	w.flush();
    	w.close();
    }
    
    
    /**
     * Read the input data set to extract data information
     * </br>and construct some data structures for following efficient processing.
     * @return running time
     * @throws IOException
     * @throws DataFormatException 
     */
	protected long preprocessing() throws IOException, DataFormatException {
    	long start = System.currentTimeMillis();
    	
    	DataReader dr = DataReader.getDataReader(this.train_filename);
    	if(dr == null){
    		System.out.println("Can not recognize the file type.");
    		return 0;
    	}
    	
    	dr.fetch_info(this.train_filename, this.target_attr_count, 0.001, false);
		
		this.attributes = dr.getAttributes();
		
		// List of all atom selectors
		this.atom_selectors = dr.getAtomSelectors();
		
		// List of all frequent selectors
		this.constructing_selectors = dr.getConstructingSelectors();
		
		this.row_count = dr.getRowCount();
		this.min_sup_count = dr.getMinSupCount();
		
		this.attr_count = dr.getAttrCount();
		this.predict_attr_count = dr.getPredictAttrCount();
		this.target_attr_count = dr.getTargetAttrCount();
		this.numeric_attr_count = dr.getNumericAttrCount();
		this.distinct_value_count = dr.getDistinctValueCount();
		
		this.selector_count = dr.getSelectorCount();
		this.predict_selector_count = dr.getPredictSelectorCount();
		this.target_selector_count = dr.getTargetSelectorCount();
		
		this.classIDs = this.get_class_ids();	// all class IDs
	    this.default_classID = this.get_default_class();	// default class ID
		
        return System.currentTimeMillis() - start;
    }
	
	protected List<Integer> get_class_ids(){
    	List<Integer> classIDs = new ArrayList<Integer>(this.target_selector_count);
    	
    	for(int i=this.predict_selector_count; i<this.selector_count; i++){
    		classIDs.add(this.constructing_selectors.get(i).selectorID);
    	}
    	
    	return classIDs;
    }
    
    protected int get_default_class(){
    	int default_classID = -1;
    	int max_support = 0;
    	for(int i=this.predict_selector_count; i<this.selector_count; i++){
    		if (max_support < this.constructing_selectors.get(i).frequency){
    			default_classID = i;
    			max_support = this.constructing_selectors.get(i).frequency;
    		}
    	}
    	return default_classID;
    }
	
	/**
	 * Read the input data set the second time to build a tree to construct N-list structures
	 * @return running time
	 * @throws IOException
	 * @throws DataFormatException 
	 */
	protected long construct_tree(PPCTree ppcTree) throws IOException, DataFormatException {
		long start = System.currentTimeMillis();  
		
		int[][] result = new int[this.row_count][];
		int index = 0;
	    
		DataReader dr = DataReader.getDataReader(this.train_filename);
		dr.bind_datasource(this.train_filename);
		
		String[] value_record;
		
		int[] id_buffer = new int[this.attr_count];
		int[] id_record;
		
		while((value_record = dr.next_record()) != null){
			// convert value_record to a record of selectorIDs
			result[index] = id_record = this.convert_values_to_selectorIDs(value_record, id_buffer);
			index++;
			
			// selectors with higher frequencies have greater selector ID
			// only support ascending sort, so the order of ids to insert to the tree is from right to left
			// since id of a target selector is always greater than id of predicting selector
			// sorting id_record will NOT blend the IDs of two kinds of selectors together	
			Arrays.sort(id_record);
			
			// System.out.println(Arrays.toString(id_record));	// for testing
			
			ppcTree.insert_record(id_record);
		}
		
		this.selectorID_records = result;
	    
		// Assign a pair of pre-order and pos-order codes for each tree node.
		ppcTree.assignPrePosOrderCode();
		
	    return System.currentTimeMillis() - start;
	}
	
	
	/**
	 * Convert input record of values to the corresponding record of selectorIDs
	 * </br> This function is used when disjunction selectors are NOT supported,
	 * @param record record of values read from data set
	 * @return record of ids which is selectorID of atom selectors, the length of returned record can be smaller than that of the input record.
	 */
	protected int[] convert_values_to_selectorIDs(String[] value_record, int[] id_buffer){
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
    
    ///////////////////////////////////////////// LEARNING PHASE //////////////////////////////////////////////
    /**
     * Learning a rule set from the training data set fetched in. 
     * @param metric
     * @param arg
     * @return the running time
     */
    public abstract long learning(METRIC_TYPES metric_type, double arg);
    
    ///////////////////////////////////////////// PREDICTION PHASE //////////////////////////////////////////////
    /**
     * Predict class id of a new example 'value_record'
     * @param value_record a new example, assume that its class is at the last position
     * @param predicted_classID the predicted class ID
     * @return the array of selector IDs of 'value_record', and the predicted class id in 'predicted_classID'
     */
    public abstract int[] predict(String[] value_record, IntHolder predicted_classID);
}
