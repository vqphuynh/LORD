/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package export.onehot;
import prepr.Selector;

/**
 * This class support to export one hot encoding for training example and the
 * generated rules.
 */
public class SelectorOneHot extends Selector {
	/**
	 * Property of atom selectors
	 * </br>The one-hot encoding of the selector
	 */
	public int onehot = -1;
	public int onehot_offset = -1;
	
	public SelectorOneHot(int attribute_id, String attribute_name, String condition, int onehot, int onehot_offset){
		super(attribute_id, attribute_name, condition);
		this.onehot = onehot;
		this.onehot_offset = onehot_offset;
	}

	public SelectorOneHot(int attribute_id, String attribute_name, String distinctValue, int frequency, int onehot, int onehot_offset) {
		super(attribute_id, attribute_name, distinctValue, frequency);
		this.onehot = onehot;
		this.onehot_offset = onehot_offset;
	}

}
