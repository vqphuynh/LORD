/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package prepr;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.DataFormatException;

import rl.IntHolder;
import weka.core.Instance;
import weka.core.Instances;
import discretizer.Discretizer;
import discretizer.Discretizer.DISCRETIZER;
import discretizer.FUSINTERDiscretizer;
import discretizer.MDLPDiscretizer;

/**
 * This adaptation class is used to interface with WEKA,
 * get information from WEKA Instances instead of from an input file as done by class <b>DataReader</b>.
 * </br>Inherit the internal class <b>DataReader</b>
 * </br>Numeric attributes will be discretized automatically by FUSINTER method.
 */
public class InstanceReader extends DataReader {
	
	private DISCRETIZER discret_method = DISCRETIZER.FUSINTER;

	public InstanceReader(){}
	
	/**
	 * Do nothing for this implementation of the abstract function defined by DataReader class
	 */
	@Override
	public void bind_datasource(String data_filename) throws DataFormatException, IOException {
		
	}
	
	/**
	 * Do nothing for this implementation of the abstract function defined by DataReader class
	 */
	@Override
	public void bind_datasource(InputStream data_stream)
			throws DataFormatException, IOException {
		
	}
	
	/**
	 * Do nothing for this implementation of the abstract function defined by DataReader class
	 */
	@Override
	public void fetch_info(String data_filename,
			int target_attr_count,
			double support_threshold,
			boolean disj_supp) throws DataFormatException, IOException{
		
	}
	
	/**
	 * Do nothing for this implementation of the abstract function defined by DataReader class
	 */
	@Override
	public void fetch_info(InputStream data_stream, 
			int target_attr_count,
			double support_threshold, 
			boolean internal_dnf) throws DataFormatException, IOException {
		
	}
	
	private void check_instances(Instances instances) throws Exception{
		if(instances.classIndex() != instances.numAttributes()-1){
			throw new Exception("Class attribute must stay at the last index.");
		}
		if(instances.classAttribute().isNumeric()){
			throw new Exception("Class attribute is a numeric attribute, change it to a nominal one, e.g. values 1, 2 --> c1, c2");
		}
	}
	
	/**
	 * Get information from Instances, be sure that the class attribute stays at the last index and is not a numeric one.
	 * @param instances
	 * @param support_threshold
	 * @throws Exception 
	 */
	
	/**
	 * Get information from Instances, be sure that the class attribute stays at the last index and is not a numeric one.
	 * </br> Note: if <b> 'discretize_attr'  = true</b>, numeric attributes will be discretized. Otherwise, they are treated as nominal ones.
	 * </br> Some data sets contain attribute values which are index of values of nominal attributes. So they should be treated as nominal rather
	 * than numeric attributes, e.g for 'lymphography' data set, 'discretize_attr' should be set to false.
	 * @param instances
	 * @param support_threshold to filter out some low frequent attribute values.
	 * @param discretize_attr
	 * @throws Exception
	 */
	public void fetch_info(Instances instances, double support_threshold, boolean discretize_attr) throws Exception{
		
		this.check_instances(instances);
		
		/**
		 * 1. Get the list of attributes
		 */
		this.attr_count = instances.numAttributes();
		this.target_attr_count = 1;
		this.predict_attr_count = this.attr_count - this.target_attr_count;
		int last_attr_index = this.attr_count -1;
		
		weka.core.Attribute weka_attr;
		for(int attr_id=0; attr_id < this.attr_count; attr_id++){
			weka_attr = instances.attribute(attr_id);
			
			if(weka_attr.isNumeric()){
				this.numeric_attr_count++;
				Attribute attr = new Attribute(attr_id, weka_attr.name(),
												Attribute.DATA_TYPE.NUMERIC);
				this.attributes.add(attr);
				continue;
			}
			
			// treat as a nominal attribute
			Attribute attr = new Attribute(attr_id, weka_attr.name(),
											Attribute.DATA_TYPE.NOMINAL,
											new HashMap<String, Selector>());
			this.attributes.add(attr);
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
		 * 3. Traverse on instances, count frequency of distinct values of nominal attributes
		 * and cache all values of numeric attributes
		 */
		IntegerArray classId_of_instances = new IntegerArray();
		for(Instance inst : instances){
	        Attribute attr;
	        String str_value;
	        
	        for(int attr_index=0; attr_index < this.attr_count; attr_index++){
	        	attr = this.attributes.get(attr_index);
	        	
	        	if(attr.type == Attribute.DATA_TYPE.NUMERIC){
	        		// numeric attribute case, record all values including null value (represented as Double.NaN)
	        		numeric_attr_values[attr_index].add(inst.value(attr_index));
	        	}else{
	        		// treat as a nominal attribute
	        		str_value = inst.stringValue(attr_index);
	        		if(Attribute.NULL_SYMBOLS.contains(str_value)) continue;	// ignore null values
	        		
	        		Selector s = attr.distinct_values.get(str_value);
	        		if(s == null){
	        			attr.distinct_values.put(str_value, new Selector(attr_index, attr.name, str_value, 1));
	        		}else{
	        			s.frequency++;
	        		}
	        	}
	        }
	        
	        // record classID of the current example, the last attribute
	        classId_of_instances.add((int)inst.value(last_attr_index));
		}
		
	    
	    // minimum support count, get the floor value
		this.row_count = instances.size();
	    this.min_sup_count = (int) (this.row_count*support_threshold);
	    
	    /**
	     * 4. Discretize all numeric attributes from the inputs
	     * 		DoubleArray[] numeric_attr_values
	     * 		IntegerArray classId_of_instances
	     * The numeric attribute values at index i are corresponding to classID at index i
	     */
	    if (discretize_attr){
	    	long discretization_time = 
		    		this.discretize_numeric_attributes(numeric_attr_values,
											    		this.attributes.get(last_attr_index).distinct_values.size(),
											    		classId_of_instances.toArray());
		    System.out.println("Discretization time: " + discretization_time + " ms");
	    }else{
	    	// Treat numeric attributes as nominal ones.
		    for(int attr_index=0; attr_index < this.attr_count; attr_index++){
	        	Attribute attr = this.attributes.get(attr_index);
	        	if(attr.type == Attribute.DATA_TYPE.NUMERIC){
	        		attr.update_distinct_values_from_discretized_values(new double[]{}, numeric_attr_values[attr_index].toArray());
	        	}
		    }
	    }
	    
	    /**
	     * 5. Prepare selector structures
	     */
	    super.prepare_selectors();
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
