/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl.eg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import prepr.Attribute;
import prepr.InstanceReader;
import prepr.Selector;
import rl.IntHolder;
import rl.PPCTree;
import rl.RuleSearcher;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import arg.Arguments;
import arg.WLordArgHelper;
import evaluations.HeuristicMetricFactory.METRIC_TYPES;


/**
 * This class inherits from and adapts class Lord (which is for multi-thread LORD algorithm) to interface with WEKA library
 * </br>Implement WEKA interfaces: <b>Classifier</b>, <b>CapabilitiesHandler</b>
 */
public class WLord extends Lord implements Classifier, CapabilitiesHandler {
	private Arguments arguments;
	private Map<Integer, Double> selectorID_to_wekaClassID;
	
    public WLord(){
        super();
    }
    
    private String[] instance_to_string_array(Instance instance){
		Attribute attr;
    	String[] value_record = new String[this.attr_count];
        
        // convert the instance to the corresponding array of text values as in the original data set
        for(int attr_index=0; attr_index < this.attr_count; attr_index++){
        	attr = this.attributes.get(attr_index);
        	
        	if(attr.type == Attribute.DATA_TYPE.NUMERIC){
        		value_record[attr_index] = instance.value(attr_index)+""; // "NaN" for a null value 
        	}else{
        		// treat as a nominal attribute
        		value_record[attr_index] = instance.stringValue(attr_index);	// "?" for a null value
        	}
        }
        
        return value_record;
    }
    
	private long preprocessing(Instances instances) throws Exception {
    	long start = System.currentTimeMillis();
    	
    	InstanceReader ir = new InstanceReader();
		ir.fetch_info(instances, 0.001, arguments.discretize_attr);
		
		this.attributes = ir.getAttributes();
		
		// List of all atom selectors
		this.atom_selectors = ir.getAtomSelectors();
		
		// List of all frequent selectors
		this.constructing_selectors = ir.getConstructingSelectors();
		
		this.row_count = ir.getRowCount();
		this.min_sup_count = ir.getMinSupCount();
		
		this.attr_count = ir.getAttrCount();
		this.predict_attr_count = ir.getPredictAttrCount();
		this.target_attr_count = ir.getTargetAttrCount();
		this.numeric_attr_count = ir.getNumericAttrCount();
		this.distinct_value_count = ir.getDistinctValueCount();
		
		this.selector_count = ir.getSelectorCount();
		this.predict_selector_count = ir.getPredictSelectorCount();
		this.target_selector_count = ir.getTargetSelectorCount();
		
		this.classIDs = this.get_class_ids();				// all class IDs
	    this.default_classID = this.get_default_class();	// default class ID
	    
	    // Prepare a mapping between selectorID and WEKA class ID (value)
	    weka.core.Attribute weka_class_attr = instances.classAttribute();
	    this.selectorID_to_wekaClassID = new HashMap<Integer, Double>();
	    for(Selector s : this.attributes.get(this.attr_count-1).distinct_values.values()){
	    	this.selectorID_to_wekaClassID.put(s.selectorID, (double) weka_class_attr.indexOfValue(s.distinctValue));
	    }
		
        return System.currentTimeMillis() - start;
    }
	
	/**
	 * Build a map between selectors and the corresponding Nodelists (Nlists)
	 * @param instances
	 * @return 
	 */
	private PPCTree create_PPCtree(Instances instances){
		PPCTree ppcTree = new PPCTree();
		
		int[][] result = new int[this.row_count][];
		int index = 0;
		
		String[] value_record;
		int[] id_buffer = new int[this.attr_count];
		int[] id_record;
		
		for(Instance instance : instances){
			value_record = this.instance_to_string_array(instance);
			
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
		
		return ppcTree;
	}
    
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// IMPLEMENTATION FOR WEKA CLASSIFIER INTERFACE ///////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    
	/**
	 * Set options for WLord.
	 * </br> '-tc' the number of threads to run, the default value is the number of physical cores.
	 * </br> '-mt' metric (heuristic) type, the default value is MESTIMATE
	 * </br> '-ma' metric argument, the default value for MESTIMATE is 0.1
	 * </br> '-da' whether doing discretization for numeric attributes, the default value is 'true'.
	 * Some data sets contain attribute values which are index of values of nominal attributes. 
	 * So they should be treated as nominal rather than numeric attributes, 
	 * e.g for 'lymphography' data set, it should 'false'.
	 * @param options
	 * @throws Exception
	 */
    public void setOptions(String[] options) throws Exception{
    	this.arguments = new Arguments();
        this.arguments.metric_type = METRIC_TYPES.MESTIMATE;	// default heuristic
        this.arguments.metric_arg = 0.1;						// default argument for mestimate heuristic
        
        this.arguments.parse(options, new WLordArgHelper());
    }
    
	@Override
	public void buildClassifier(Instances instances) throws Exception {
		if(this.arguments == null){
			this.setOptions(new String[]{});
		}
		
		this.preprocessing(instances);
		
		PPCTree ppcTree = this.create_PPCtree(instances);
		this.selector_nodelists = ppcTree.create_Nlist_for_selectors_arr(this.selector_count);
        this.selector_nodelist_map = ppcTree.create_selector_Nlist_map(this.selector_nodelists);
        RuleSearcher.setSelectorNodelists(this.selector_nodelists);
        
		super.learning(this.arguments.metric_type, this.arguments.metric_arg);
	}

	@Override
	public double classifyInstance(Instance instance) throws Exception {
		String[] value_record = instance_to_string_array(instance);
		
        // Predict
		IntHolder predicted_classID = new IntHolder(-1);
		super.predict(value_record, predicted_classID);	// predict internal class ID defined by LORD
		
		// convert the internal class ID defined by LORD to the internal class ID defined by WEKA
		return this.selectorID_to_wekaClassID.get(predicted_classID.value);
	}

	/**
	 * WLord does not support a distribution of membership for each instance.
	 * @param instance
	 * @return null
	 * @throws Exception
	 */
	@Override
	public double[] distributionForInstance(Instance instance) throws Exception {
		return null;
	}

	@Override
	public Capabilities getCapabilities() {	// method defined in interfaces: Classifier, CapabilitiesHandler
		Capabilities result = new Capabilities(this);
		result.disableAll();
		
		// attributes
		result.enableAllAttributes();						// enable all attribute types
		result.disable(Capability.DATE_ATTRIBUTES);			// disable date attribute
		result.disable(Capability.RELATIONAL_ATTRIBUTES);	// disable relational attribute
		result.enable(Capability.MISSING_VALUES);			// allow missing value at predicting attributes
		
		// class
		result.enableAllClasses();						// enable all class types
		result.disable(Capability.DATE_CLASS);			// disable date class
		result.disable(Capability.NUMERIC_CLASS);		// disable numeric class
		result.disable(Capability.RELATIONAL_CLASS);	// disable relational class
		
		return result;
	}
}
