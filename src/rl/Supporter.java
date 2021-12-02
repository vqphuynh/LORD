/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package rl;

import java.util.Iterator;
import java.util.List;


public class Supporter {    
    /**
     * This function generates Descarte production from two sets of sub sets.
     * This power set does not include empty set
     */
    public static void generateDescartProduction(List<String> list1, List<String> list2, List<String> result){
    	StringBuilder sb = new StringBuilder(100);
    	int length;
    	for(String s1 : list1){
    		sb.setLength(0);
    		length = sb.append(s1).append(' ').length();
    		for(String s2 : list2){
    			sb.setLength(length);
    			result.add(sb.append(s2).toString());
    		}
    	}
    }
    
    /**
     * This function generates a set of sub sets building from a list of items.
     * This power set does not include empty set
     */
    public static void generatePowerSet(List<String> items, List<String> powerSet) throws NullPointerException{
    	if(items == null || items.size()==0) return;
    	
    	// Initialize for the powerSet with the first item in the items
    	Iterator<String> iterator = items.iterator();
    	powerSet.add(iterator.next());
    	
    	while(iterator.hasNext()) addNewCombinations(iterator.next(), powerSet);
    }
    
    private static void addNewCombinations(String item, List<String> powerSet){
    	int L = powerSet.size();
    	powerSet.add(item);
    	StringBuilder sb = new StringBuilder(100);
    	for(int i=0; i<L; i++){
    		sb.setLength(0);
    		powerSet.add(sb.append(powerSet.get(i)).append(' ').append(item).toString());
    	}
    }
     
    /**
     * Calculate the Nlist of itemsets common|i1|i2 from two Nlists of 2 itemsets common|i1, common|i2. (i1 < i2, common can be empty)
     * </br>Or calculate the Nlist of itemset [itemset][item], e.g. abcde, from 2 Nlists of itemset abcd and item e, assume that a < b < c < d < e
     * </br> <b>Note: not commutative</b> between nodelist1 and nodelist2
     * @param nodelist1 of itemset common|i1 or [itemset]
     * @param nodelist2 of itemset common|i2 or [item]
     * @return the nodelist of itemset common|i1|i2 or [itemset][item]
     */
    public static Nodelist create_nlist(Nodelist nodelist1, Nodelist nodelist2){
    	int size1 = nodelist1.size(), size2 = nodelist2.size();
    	if(size1 == 0 || size2 == 0) return new NodelistEmpty();
		
    	int index1=0, index2=0, parent_node_index = -1, parent_node_pre = -1;
    	Nodelist nodelist = new Nodelist(size1);
		Node i1_node = new Node(), i2_node = new Node();
		nodelist1.get(index1, i1_node);
		nodelist2.get(index2, i2_node);
		
    	while(true){
    		if(i1_node.pre > i2_node.pre){
    			if(i1_node.pos < i2_node.pos){
    				// This desired case says that: i1_node is a descendant of i2_node. 
    				// So i2_node (ancestor) is added to the node list of i1i2 itemset --> increase index1
    				// NOTE: i2_node can be an ancestor of other nodes in i1_nodelist --> stay index2
    				if(parent_node_pre == i2_node.pre){
    					nodelist.accSupportCount(parent_node_index, i1_node.count);
    				}else{
    					nodelist.add(i2_node.pre, i2_node.pos, i1_node.count);
    					parent_node_pre = i2_node.pre;
    					parent_node_index++;
    				}
    				index1++;
    				if(index1 < size1) nodelist1.get(index1, i1_node);
    				else break;
    			}else{ // i1_node.pre > i2_node.pre && i1_node.pos > i2_node.pos
    				// This undesired case says that: 
    				// All nodes from i1_node in nodelist1 are NOT descendant of i2_node --> increase index2
    				// but i1_node can be a descendant of other nodes in nodelist2 --> stay index1
    				index2++;
    				if(index2 < size2) nodelist2.get(index2, i2_node);
    				else break;
    			}
    		}else{ // i1_node.pre < i2_node.pre --> it must be i1_node.pos < i2_node.pos
    			// This undesired case says that: 
				// All nodes from i2_node in nodelist2 are not ancestors of i1_node --> increase index1
				// but i2_node can be a ancestor of other nodes in nodelist1 --> stay index2
    			index1++;
    			if(index1 < size1) nodelist1.get(index1, i1_node);
				else break;
    		}
    	}
    	
    	//nodelist.shrink();	// for memory save
    	
    	return nodelist;
    }
    
    /**
     * 'And' operation between two boolean expressions each of which is represented by an Nlist.
     * </br> The operator is commutative between nodelist1 and nodelist2
     * @param nodelist1 Nlist of boolean expression 1
     * @param nodelist2 Nlist of boolean expression 2
     * @return The representing Nlist of the result 'And' boolean expression
     */
    public static Nodelist create_nlist_conj(Nodelist nodelist1, Nodelist nodelist2){
    	int size1 = nodelist1.size(), size2 = nodelist2.size();
    	if(size1 == 0 || size2 == 0) return new NodelistEmpty();
		
    	int index1=0, index2=0;
		Nodelist nodelist = new Nodelist((size2 > size1) ? size2 : size1);
		Node i1_node = new Node(), i2_node = new Node();
		nodelist1.get(index1, i1_node);
		nodelist2.get(index2, i2_node);
		
		/*
		 * Operation principle:
		 * If having an ancestor-descendant relationship, add the descendant node
		 */
		
    	while(true){
    		if(i1_node.pre > i2_node.pre){
    			if(i1_node.pos < i2_node.pos){
    				// ancestor-descendant relationship (i1_node.pre > i2_node.pre && i1_node.pos < i2_node.pos)
    				// i1_node: descendant, i2_node: ancestor
    				// add i1_node to the result nodelist, increase index1, stay index2
    				nodelist.add(i1_node);
    				index1++;
    				if(index1 < size1) nodelist1.get(index1, i1_node);
    				else break;
    			}else{
    				// NO ancestor-descendant relationship (i1_node.pre > i2_node.pre && i1_node.pos > i2_node.pos) 
    				// all nodes from i1_node in nodelist1 are NOT descendant of i2_node --> increase index2
    				// but i1_node can be a descendant of other nodes in nodelist2 --> stay index1
    				index2++;
    				if(index2 < size2) nodelist2.get(index2, i2_node);
    				else break;
    			}
    		}else if(i1_node.pre < i2_node.pre){
    			if(i1_node.pos < i2_node.pos){
    				// NO ancestor-descendant relationship (i1_node.pre < i2_node.pre && i1_node.pos < i2_node.pos)
    				// all nodes from i2_node in nodelist2 are NOT descendant of i1_node --> increase index1
    				// but i2_node can be a descendant of other nodes in nodelist1 --> stay index2
    				index1++;
    				if(index1 < size1) nodelist1.get(index1, i1_node);
    				else break;
    			}else{
    				// ancestor-descendant relationship (i1_node.pre < i2_node.pre && i1_node.pos > i2_node.pos)
    				// i1_node: ancestor, i2_node: descendant
    				// add i2_node to the result nodelist, increase index2, stay index1
    				nodelist.add(i2_node);
    				index2++;
    				if(index2 < size2) nodelist2.get(index2, i2_node);
    				else break;
    			}
    		}else{
    			// identical nodes
    			// i1_node.pre == i2_node.pre --> i1_node.pos == i2_node.pos
    			nodelist.add(i1_node);
    			index1++;
				if(index1 < size1) nodelist1.get(index1, i1_node);
				else break;
				
				index2++;
				if(index2 < size2) nodelist2.get(index2, i2_node);
				else break;
    		}
    	}
    	
    	nodelist.shrink();	// for memory save
    	
    	return nodelist;
    	
    }
    
    /**
     * 'Or' operation between two boolean expressions each of which is represented by an Nlist.
     * </br> The operator is commutative between nodelist1 and nodelist2
     * @param nodelist1 Nlist of boolean expression 1
     * @param nodelist2 Nlist of boolean expression 2
     * @return The representing Nlist of the result 'Or' boolean expression
     */
    public static Nodelist create_nlist_disj(Nodelist nodelist1, Nodelist nodelist2){
    	int size1 = nodelist1.size(), size2 = nodelist2.size();
    	if(size1 == 0 || size2 == 0) return new NodelistEmpty();
		
    	int index1=0, index2=0, ancestor_node_pre=-1;
		Nodelist nodelist = new Nodelist(size1+size2);
		Node i1_node = new Node(), i2_node = new Node();
		nodelist1.get(index1, i1_node);
		nodelist2.get(index2, i2_node);
		
		/*
		 * Operation principle:
		 * If having an ancestor-descendant relationship, add the ancestor node (add only once)
		 * Add all other nodes that do not have ancestor-descendant relationships
		 */
		
    	while(true){
    		if(i1_node.pre > i2_node.pre){
    			if(i1_node.pos < i2_node.pos){
    				// ancestor-descendant relationship (i1_node.pre > i2_node.pre && i1_node.pos < i2_node.pos)
    				// i1_node: descendant, i2_node: ancestor
    				// add i2_node to the result nodelist if it is not added yet
    				// stay index2 because i2_node can be ancestor of other nodes in nodelist1, increase index1
    				
    				if(ancestor_node_pre != i2_node.pre){
    					nodelist.add(i2_node);
    					ancestor_node_pre = i2_node.pre;
    				}
    				
    				index1++;
    				if(index1 < size1) nodelist1.get(index1, i1_node);
    				else {
    					index2++;	// ancestor has added
    					break;
    				}
    			}else{
    				// NO ancestor-descendant relationship (i1_node.pre > i2_node.pre && i1_node.pos > i2_node.pos) 
    				// add i2_node
    				// all nodes from i1_node in nodelist1 are NOT descendant of i2_node --> increase index2
    				// but i1_node can be a descendant of other nodes in nodelist2 --> stay index1
    				
    				// adding if i2_node is not an ancestor added previously
    				if(ancestor_node_pre != i2_node.pre) nodelist.add(i2_node);	
    				
    				index2++;
    				if(index2 < size2) nodelist2.get(index2, i2_node);
    				else break;
    			}
    		}else if(i1_node.pre < i2_node.pre){
    			if(i1_node.pos < i2_node.pos){
    				// NO ancestor-descendant relationship (i1_node.pre < i2_node.pre && i1_node.pos < i2_node.pos)
    				// add i1_node
    				// all nodes from i2_node in nodelist2 are NOT descendant of i1_node --> increase index1
    				// but i2_node can be a descendant of other nodes in nodelist1 --> stay index2
    				
    				// adding if i1_node is not an ancestor added previously
    				if(ancestor_node_pre != i1_node.pre) nodelist.add(i1_node);
    				
    				index1++;
    				if(index1 < size1) nodelist1.get(index1, i1_node);
    				else break;
    			}else{
    				// ancestor-descendant relationship (i1_node.pre < i2_node.pre && i1_node.pos > i2_node.pos)
    				// i1_node: ancestor, i2_node: descendant
    				// add i1_node to the result nodelist if it is not added yet
    				// stay index1 because i1_node can be ancestor of other nodes in nodelist2, increase index2
    				
    				if(ancestor_node_pre != i1_node.pre){
    					nodelist.add(i1_node);
    					ancestor_node_pre = i1_node.pre;
    				}
    				
    				index2++;
    				if(index2 < size2) nodelist2.get(index2, i2_node);
    				else{
    					index1++; // ancestor has added
    					break;
    				}
    			}
    		}else{
    			// identical nodes
    			// i1_node.pre == i2_node.pre --> i1_node.pos == i2_node.pos
    			nodelist.add(i1_node);
    			index1++;
    			index2++;
    			
				if(index1 < size1) nodelist1.get(index1, i1_node);
				else break;
				
				if(index2 < size2) nodelist2.get(index2, i2_node);
				else break;
    		}
    	}
    	
    	// add the remaining nodes in one of the two input node lists
    	if(index1 < size1){
    		for(int i=index1; i<size1; i++){
    			nodelist1.get(i, i1_node);
    			nodelist.add(i1_node);
    		}
    		return nodelist;
		}
    	
    	if(index2 < size2){
    		for(int i=index2; i<size2; i++){
    			nodelist2.get(i, i2_node);
    			nodelist.add(i2_node);
    		}
		}
    	
    	nodelist.shrink();	// for memory save
    	
    	return nodelist;
    	
    }
    
    /**
     * Convert string s into integer array
     * @param s String of integers with space characters as delimiters
     * @return
     */
    public static int[] to_integer_array(String s){
    	String[] str_ids = s.split(" ");
    	int[] ids = new int[str_ids.length];
    	int i = 0;
    	for(String str : str_ids){
    		ids[i] = Integer.parseInt(str);
    		i++;
    	}
    	return ids;
    }
    
    /**
     * return array of statistic information: [max, min, mean, stdev]
     * @param numbers
     * @return
     */
    public static double[] get_statistic_info(double[] numbers){
    	double[] results = new double[4];
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        
        for (double num : numbers){
        	sum += num;
        	if(min > num) min = num;
        	if(max < num) max = num;
        }
        
        results[0] = max;
        results[1] = min;
        double mean = sum/numbers.length;
        results[2] = mean;
        
        double diff;
        double total_square_diff = 0;
        for (double num : numbers) {
        	diff = num - mean;
        	total_square_diff += diff*diff;
        }
        
        results[3] = Math.sqrt(total_square_diff/numbers.length);
        
        return results;
    }
}
