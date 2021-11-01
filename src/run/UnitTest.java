/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.DataFormatException;

import prepr.DataReader;
import prepr.Selector;
import rl.Nodelist;
import rl.PPCTree;
import rl.Supporter;

class UnitTest {	
	public static void main(String[] args) throws DataFormatException, IOException{
		test_nlist_for_boolean_exp();
	}
	
	public static void test_nlist_for_boolean_exp() throws DataFormatException, IOException{
		String filename = "data/inputs/datasets/examples.csv";
		DataReader info = DataReader.getDataReader(filename);
		info.fetch_info(filename, 1, 0.001, false);
		
		PPCTree ppcTree = new PPCTree(); 
		DataReader dr = DataReader.getDataReader(filename);
		dr.bind_datasource(filename);
		
		String[] value_record;
		
		int[] id_buffer = new int[info.getAttrCount()];
		int[] id_record;
		
		while((value_record = dr.next_record()) != null){
			// convert value_record to a record of selectorIDs
			id_record = info.convert_values_to_selectorIDs(value_record, id_buffer);
			
			// selectors with higher frequencies have greater selector ID
			// only support ascending sort, so the order of ids to insert to the tree is from right to left
			// since id of a target selector is always greater than id of predicting selector
			// sorting id_record will NOT blend the IDs of two kinds of selectors together
			Arrays.sort(id_record);
			
			// System.out.println(Arrays.toString(id_record));	// for testing
			
			ppcTree.insert_record(id_record);
		}
	    
		// Assign a pair of pre-order and pos-order codes for each tree node.
		ppcTree.assignPrePosOrderCode();
		ppcTree.storeTree("data/outputs/tree.txt");
		
		// print selector-Nlist
		Map<String, Nodelist> selector_nodelist_map = ppcTree.create_selector_Nlist_map(info.getSelectorCount());	
		System.out.println("Selector-Nlist:");
		for(Selector s : info.getConstructingSelectors()){
			Nodelist nlist = selector_nodelist_map.get(Arrays.toString(new int[]{s.selectorID}));
			System.out.print(s.selectorID + "-" + s.condition);
			System.out.print(": ");
			System.out.println(nlist.toString());
		}
		
		Nodelist nlist0 = selector_nodelist_map.get(Arrays.toString(new int[]{0}));
		Nodelist nlist1 = selector_nodelist_map.get(Arrays.toString(new int[]{1}));
		Nodelist nlist2 = selector_nodelist_map.get(Arrays.toString(new int[]{2}));
		Nodelist nlist3 = selector_nodelist_map.get(Arrays.toString(new int[]{3}));
		Nodelist nlist4 = selector_nodelist_map.get(Arrays.toString(new int[]{4}));
		Nodelist nlist5 = selector_nodelist_map.get(Arrays.toString(new int[]{5}));
		Nodelist nlist6 = selector_nodelist_map.get(Arrays.toString(new int[]{6}));
		Nodelist nlist7 = selector_nodelist_map.get(Arrays.toString(new int[]{7}));
		Nodelist nlist8 = selector_nodelist_map.get(Arrays.toString(new int[]{8}));
		Nodelist nlist9 = selector_nodelist_map.get(Arrays.toString(new int[]{9}));
		Nodelist nlist10 = selector_nodelist_map.get(Arrays.toString(new int[]{10}));
		Nodelist nlist11 = selector_nodelist_map.get(Arrays.toString(new int[]{11}));
		Nodelist nlist12 = selector_nodelist_map.get(Arrays.toString(new int[]{12}));
		Nodelist nlist13 = selector_nodelist_map.get(Arrays.toString(new int[]{13}));
		
		/*
		10-(a1=1): {<12,13>:1; <27,32>:3; <36,49>:9} sc:13
		2-(a1=2): {<6,1>:1; <11,6>:1; <20,15>:1; <22,17>:1; <26,21>:1} sc:5
		0-(a1=3): {<5,0>:1; <10,5>:1} sc:2

		9-(a2=1): {<7,9>:2; <37,48>:9} sc:11
		6-(a2=2): {<3,3>:2; <14,11>:1; <18,19>:2; <25,22>:1; <29,27>:2; <34,29>:1} sc:9

		1-(a3=1): {<39,34>:1; <41,36>:1; <49,44>:1} sc:3
		7-(a3=2): {<17,20>:2; <24,23>:1; <28,28>:2; <33,30>:1; <45,43>:2; <51,46>:1} sc:9
		5-(a3=3): {<4,2>:2; <9,7>:2; <15,10>:1; <42,40>:2; <50,45>:1} sc:8

		3-(a4=1): {<19,16>:1; <30,25>:1; <38,35>:1; <43,38>:1; <46,41>:1} sc:5
		4-(a4=2): {<21,18>:1; <31,26>:1; <40,37>:1; <44,39>:1; <47,42>:1} sc:5
		8-(a4=3): {<2,4>:2; <8,8>:2; <13,12>:1; <23,24>:1; <32,31>:1; <48,47>:3} sc:10

		13-(a5=c1): {<35,50>:9} sc:9
		12-(a5=c2): {<16,33>:6} sc:6
		11-(a5=c3): {<1,14>:5} sc:5
		*/
		
		System.out.println();
		
		// The TRUE support counts of the following boolean expressions in file 'data/inputs/datasets/examples.xlsx'
		
		// exp1: (a12 || a13) && (a21 || a31 || a41) && a53 --> (2 || 0) && (9 || 1 || 3) & 11
		Nodelist nlist_or_2_0 = Supporter.create_nlist_disj(nlist2, nlist0);
		System.out.println("2: " + nlist2.toString());
		System.out.println("0: " + nlist0.toString());
		System.out.println("2 || 0: " + nlist_or_2_0.toString());
		
		Nodelist nlist_or_9_1 = Supporter.create_nlist_disj(nlist9, nlist1);
		System.out.println("9: " + nlist9.toString());
		System.out.println("1: " + nlist1.toString());
		System.out.println("9 || 1: " + nlist_or_9_1.toString());
		
		Nodelist nlist_or_9_1_3 = Supporter.create_nlist_disj(nlist_or_9_1, nlist3);
		System.out.println("3: " + nlist3.toString());
		System.out.println("9 || 1 || 3: " + nlist_or_9_1_3.toString());
		
		Nodelist nlist_and_or_2_0_or_9_1_3 = Supporter.create_nlist_conj(nlist_or_2_0, nlist_or_9_1_3);
		System.out.println("2 || 0: " + nlist_or_2_0.toString());
		System.out.println("(2 || 0) && (9 || 1 || 3): " + nlist_and_or_2_0_or_9_1_3.toString());
		
		Nodelist nlist_exp1 = Supporter.create_nlist_conj(nlist_and_or_2_0_or_9_1_3, nlist11);
		System.out.println("11: " + nlist11.toString());
		System.out.println("exp1 = (2 || 0) && (9 || 1 || 3) && 11 : " + nlist_exp1.toString());
		
		System.out.println();
		
		// exp2: (a11 && a22 & a33) || (a33 & a43) || a52 	--> (10 && 6 && 5) || (5 & 8) || 12
		Nodelist nlist_and_10_6 = Supporter.create_nlist_conj(nlist10, nlist6);
		System.out.println("10: " + nlist10.toString());
		System.out.println("6: " + nlist6.toString());
		System.out.println("10 && 6: " + nlist_and_10_6.toString());
		
		System.out.println("5: " + nlist5.toString());
		Nodelist nlist_and_10_6_5 = Supporter.create_nlist_conj(nlist_and_10_6, nlist5);
		System.out.println("10 && 6 && 5: " + nlist_and_10_6_5.toString());
		
		Nodelist nlist_and_5_8 = Supporter.create_nlist_conj(nlist5, nlist8);
		System.out.println("5: " + nlist5.toString());
		System.out.println("8: " + nlist8.toString());
		System.out.println("5 && 8: " + nlist_and_5_8.toString());
		
		Nodelist nlist_or_and_10_6_5_and_5_8 = Supporter.create_nlist_disj(nlist_and_10_6_5, nlist_and_5_8);
		System.out.println("10 && 6 && 5: " + nlist_and_10_6_5.toString());
		System.out.println("(10 && 6 && 5) || (5 & 8): " + nlist_or_and_10_6_5_and_5_8.toString());
		
		Nodelist nlist_exp2 = Supporter.create_nlist_disj(nlist_or_and_10_6_5_and_5_8, nlist12);
		System.out.println("12: " + nlist12.toString());
		System.out.println("exp2 = (10 && 6 && 5) || (5 & 8) || 12: " + nlist_exp2.toString());
		
		System.out.println();
		
		// exp3: a32 || a42 && a51							--> 7 || 4 && 13
		Nodelist nlist_7_or_4 = Supporter.create_nlist_disj(nlist7, nlist4);
		System.out.println("7: " + nlist7.toString());
		System.out.println("4: " + nlist4.toString());
		System.out.println("7 || 4: " + nlist_7_or_4.toString());
		
		Nodelist nlist_exp3 = Supporter.create_nlist_conj(nlist_7_or_4, nlist13);
		System.out.println("13: " + nlist13.toString());
		System.out.println("exp3 = 7 || 4 && 13: " + nlist_exp3.toString());
		
		System.out.println();
		
		// exp4: exp1 && exp2 && exp3
		System.out.println("exp1: " + nlist_exp1.toString());
		System.out.println("exp2: " + nlist_exp2.toString());
		Nodelist nlist_exp1_and_exp2 = Supporter.create_nlist_conj(nlist_exp1, nlist_exp2);
		System.out.println("exp1 && exp2: " + nlist_exp1_and_exp2.toString());
		System.out.println("exp3: " + nlist_exp3.toString());
		Nodelist nlist_exp1_and_exp2_and_exp3 = Supporter.create_nlist_conj(nlist_exp1_and_exp2, nlist_exp3);
		System.out.println("exp4 = exp1 && exp2 && exp3: " + nlist_exp1_and_exp2_and_exp3.toString());
		
		System.out.println();
		
		// exp5: exp1 || exp2 || exp3
		System.out.println("exp1: " + nlist_exp1.toString());
		System.out.println("exp2: " + nlist_exp2.toString());
		Nodelist nlist_exp1_or_exp2 = Supporter.create_nlist_disj(nlist_exp1, nlist_exp2);
		System.out.println("exp1 || exp2: " + nlist_exp1_or_exp2.toString());
		System.out.println("exp3: " + nlist_exp3.toString());
		Nodelist nlist_exp1_or_exp2_or_exp3 = Supporter.create_nlist_disj(nlist_exp1_or_exp2, nlist_exp3);
		System.out.println("exp5 = exp1 || exp2 || exp3: " + nlist_exp1_or_exp2_or_exp3.toString());

	}
}


