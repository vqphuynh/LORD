/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package export.onehot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;

import prepr.Attribute;
import prepr.CSVReader;
import prepr.DataReader;
import prepr.Selector;
import rl.IntHolder;
import rl.PPCTree;
import rl.RuleInfo;
import rl.eg.Lord;

public class LordOneHotExport extends Lord {
	protected int[][] distinctValueID_records;	// for train examples
	protected List<int[]> test_examples = new ArrayList<int[]>();	// for test examples
	
	public LordOneHotExport(){
        super();
    }
	
	
	/**
	 * Convert input record of values to the corresponding record of distinctValueID
	 * @param value_record is a record of values read from data set
	 * @return record of distinctValueID, the length of returned record can be smaller than that of the input record because of null values
	 */
	public int[] convert_values_to_distinctValueIDs(String[] value_record, int[] id_buffer){
		int count=0;
		Selector s;
		
		for(int i=0; i<value_record.length; i++){
			s = this.attributes.get(i).getSelector(value_record[i]);
			if(s != null){
				id_buffer[count] = s.distinctValueID;
				count++;
			}
		}
		
		int[] id_record = new int[count];
		System.arraycopy(id_buffer, 0, id_record, 0, count);
		
		return id_record;
	}
	
	public int[] predict(String[] value_record, IntHolder predicted_classID){
		// conduct a record of distinct value IDs for the test example
		int[] id_buffer = new int[this.selector_count];
		int [] test_example = this.convert_values_to_distinctValueIDs(value_record, id_buffer);
		this.test_examples.add(test_example);
		
    	return super.predict(value_record, predicted_classID);
    }
	
	public int[] predict_noclass(String[] value_record, IntHolder predicted_classID){
		// conduct a record of distinct value IDs for the test example
		int[] id_buffer = new int[this.selector_count];
		int [] test_example = this.convert_values_to_distinctValueIDs(value_record, id_buffer);
		this.test_examples.add(test_example);
		
    	return super.predict_noclass(value_record, predicted_classID);
    }
	
	
	/**
	 * Read the input data set the second time to build a tree to construct N-list structures
	 * @return running time
	 * @throws IOException
	 * @throws DataFormatException 
	 */
	protected long construct_tree(PPCTree ppcTree) throws IOException, DataFormatException {
		long start = System.currentTimeMillis();
		
		DataReader dr = null;
		if(this.train_filename != null){
			dr = DataReader.getDataReader(this.train_filename);
			dr.bind_datasource(this.train_filename);
		}else if (this.data_stream != null){
			dr = new CSVReader();
			this.data_stream.reset();	// ByteArrayInputStream can support reset()
			dr.bind_datasource(this.data_stream);
		}else{
    		System.out.println("No train data");
    		return 0;
    	}
		
		int[][] result1 = new int[this.row_count][];
		int[][] result2 = new int[this.row_count][];
		int index = 0;
		String[] value_record;
		
		//int[] id_buffer = new int[this.attr_count];
		int[] id_buffer = new int[this.selector_count]; // to support PosNegFeatureRuleLearner
		int[] id_record;
		
		while((value_record = dr.next_record()) != null){
			// convert value_record to a record of selectorIDs
			result1[index] = id_record = this.convert_values_to_selectorIDs(value_record, id_buffer);
			// convert value_record to a record of selectorIDs
			result2[index] = this.convert_values_to_distinctValueIDs(value_record, id_buffer);
			index++;
			
			// selectors with higher frequencies have greater selector ID
			// only support ascending sort, so the order of ids to insert to the tree is from right to left
			// since id of a target selector is always greater than id of predicting selector
			// sorting id_record will NOT blend the IDs of two kinds of selectors together	
			Arrays.sort(id_record);
			
			// System.out.println(Arrays.toString(id_record));	// for testing
			
			ppcTree.insert_record(id_record);
		}
		
		this.selectorID_records = result1;
		this.distinctValueID_records = result2;
	    
		// Assign a pair of pre-order and pos-order codes for each tree node.
		ppcTree.assignPrePosOrderCode();
		
	    return System.currentTimeMillis() - start;
	}
	
	
	private void feed_true_class(String[] y_test){
		Attribute class_attr = this.attributes.get(this.attr_count-1); // class attribute at the last position
		int last_pos = this.test_examples.get(0).length-1;
		int idx = 0;
		for(int[] test_example : this.test_examples){
			test_example[last_pos] = class_attr.getSelector(y_test[idx]).distinctValueID;
			idx ++;
		}
	}
	
	
	/**
	 * EXPORT ONE-HOT FOR TRAINING EXAMPLES AND RULES
	 * @param dir_path
	 * @param y_test
	 * @throws IOException 
	 */
	public void export_onehot(String dir_path, String[] y_test) throws IOException{
		
		// Prepare a list of One-Hot-Encoding supporting Selectors.
		// The distinct value Id of a SelectorOneHot is its position in the list
		// (In the order of distinctValueID)
		SelectorOneHot[] soh_list_1 = new SelectorOneHot[this.atom_selectors.size()];
		int onehot_offset = 0;
		for(Attribute attribute : this.attributes){
			int value_count = attribute.distinct_values.size();
			int index = 0;
			for (Entry<String, Selector> entry : attribute.distinct_values.entrySet()){
				Selector s = entry.getValue();
				SelectorOneHot soh = new SelectorOneHot(s.attributeID, s.attributeName, s.distinctValue, s.frequency, index, onehot_offset);
				soh.distinctValueID = s.distinctValueID;
				soh.selectorID = s.selectorID;
				soh_list_1[soh.distinctValueID] = soh;
				index++;
			}
			onehot_offset += value_count;
		}
		
		// Prepare a list of One-Hot-Encoding supporting Selectors.
		// The selector Id of a SelectorOneHot is its position in the list
		// In the order of selectorID
		SelectorOneHot[] soh_list_2 = new SelectorOneHot[this.constructing_selectors.size()];
		for(Selector s : this.constructing_selectors){
			SelectorOneHot soh = soh_list_1[s.distinctValueID];
			soh_list_2[soh.selectorID] = soh;
		}		
				
		// Export the training examples in one-hot, the same size
		FileWriter fw = new FileWriter(Paths.get(dir_path, "train_onehot.txt").toString());
		BufferedWriter writer = new BufferedWriter(fw);
		StringBuffer sb = new StringBuffer(1024*8);
		
		for (int[] id_record : this.distinctValueID_records){
			int[] onehot_record = new int[this.distinct_value_count];
			for(int id : id_record){
				// id is distinctValueID
				SelectorOneHot soh = soh_list_1[id];
				onehot_record[soh.onehot_offset + soh.onehot] = 1;
			}
			sb.setLength(0);
			for(int bit : onehot_record){
				sb.append(bit).append(',');
			}
			sb.setLength(sb.length()-1);	// remove the last ','
			sb.append('\n');
			writer.write(sb.toString());
		}
		writer.flush();
		writer.close();
		
		
		// Export the test examples in one-hot, the same size
		fw = new FileWriter(Paths.get(dir_path, "test_onehot.txt").toString());
		writer = new BufferedWriter(fw);
		sb = new StringBuffer(1024*8);
		
		if (y_test != null){
			this.feed_true_class(y_test);
		}
		
		for (int[] id_record : this.test_examples){
			int[] onehot_record = new int[this.distinct_value_count];
			for(int id : id_record){
				// id is distinctValueID
				SelectorOneHot soh = soh_list_1[id];
				onehot_record[soh.onehot_offset + soh.onehot] = 1;
			}
			sb.setLength(0);
			for(int bit : onehot_record){
				sb.append(bit).append(',');
			}
			sb.setLength(sb.length()-1);	// remove the last ','
			sb.append('\n');
			writer.write(sb.toString());
		}
		writer.flush();
		writer.close();
		
		
		// Export the rules in one-hot, the same size
		fw = new FileWriter(Paths.get(dir_path, "rules_onehot.txt").toString());
		writer = new BufferedWriter(fw);
		
		for (RuleInfo rule : this.rm.ruleList){
			int[] onehot_record = new int[this.distinct_value_count];
			
			for(int id : rule.body){
				// id is selectorID
				SelectorOneHot soh = soh_list_2[id];
				onehot_record[soh.onehot_offset + soh.onehot] = 1;
			}
			SelectorOneHot soh = soh_list_2[rule.headID];
			onehot_record[soh.onehot_offset + soh.onehot] = 1;
			
			sb.setLength(0);
			for(int bit : onehot_record){
				sb.append(bit).append(',');
			}
			sb.setLength(sb.length()-1);	// remove the last ','
			sb.append('\n');
			writer.write(sb.toString());
		}
		writer.flush();
		writer.close();
		
		
		// Export one-hot encode-table
		fw = new FileWriter(Paths.get(dir_path, "onehot_encode_table.txt").toString());
		writer = new BufferedWriter(fw);
		
		for (Attribute att : this.attributes){
			int values_count = att.distinct_values.size();
			sb.setLength(0);
			sb.append("Attribute: ").append(att.name).append(", ValuesCount: ").append(values_count).append('\n');
			for (Selector s : att.distinct_values.values()){
				sb.append('\t').append(s.distinctValue).append(", ");
				SelectorOneHot soh = soh_list_1[s.distinctValueID];
				int[] code = new int[values_count];
				code[soh.onehot] = 1;
				for(int bit : code) sb.append(bit);
				sb.append('\n');
			}
			writer.write(sb.toString());
		}
		writer.flush();
		writer.close();
	}
}
