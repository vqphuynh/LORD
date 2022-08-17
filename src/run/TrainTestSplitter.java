/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

import java.io.File;
import java.nio.file.Paths;

import weka.core.Instances;
import weka.core.converters.AbstractFileSaver;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.instance.StratifiedRemoveFolds;

/**
 * This utility class splits an input data set into n pairs of training-testing data sets for n-fold cross-validation using WEKA lib.
 */
public class TrainTestSplitter{
	private static String datasoure_filename = "data/inputs/datasets/adult.arff";
	private static int fold_count = 10;
	private static int seed = 1;
	private static String output_format = null;
	
	public static void main(String[] args){
		try {
			if(args.length < 3){
				System.out.println("Parameters: <data filename> <number of folds> <seed> <output_format>");
				//split(datasoure_filename, fold_count, seed, output_format);	// uncomment for debugging run
				return;
			}
			datasoure_filename = args[0];
			fold_count = Integer.parseInt(args[1]);
			seed = Integer.parseInt(args[2]);
			if(args.length >= 4) output_format = args[3];
			split(datasoure_filename, fold_count, seed, output_format);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates a number of training-testing data set pairs according to Cross-Validation principle.
	 * Data set pairs are stored in the same directory
	 * @param data_filename
	 * @param folds_count
	 * @throws Exception 
	 */
	public static void split(String data_filename, int folds_count, int seed, String output_format) throws Exception{
		File data_file = new File(data_filename);
		String directory = data_file.getParent();
		String simple_filename = data_file.getName();
		String[] name_extend = simple_filename.split("\\.");
		String name = name_extend[0], extend = "";
		if(name_extend.length > 1) extend = "."+name_extend[name_extend.length-1];
		
		
		// load data  
		DataSource source = new DataSource(data_filename);
		Instances data = source.getDataSet();
		System.out.println("Total number of instances: " + data.size());
		System.out.println();

		// set class to last attribute
		if (data.classIndex() == -1)
		    data.setClassIndex(data.numAttributes() - 1);

		// use StratifiedRemoveFolds to randomly split the data  
		StratifiedRemoveFolds filter = new StratifiedRemoveFolds();
		filter.setNumFolds(folds_count);
		filter.setSeed(seed);
		
		AbstractFileSaver writer;
		if(output_format == null || output_format == ""){
			if(extend.equalsIgnoreCase(".csv")) writer = new CSVSaver();
			else if(extend.equalsIgnoreCase(".arff")) writer = new ArffSaver(); 
			else{
				System.out.println("No data format, specify .csv or .arff format");
				return;
			}
		}else{
			if(output_format.equalsIgnoreCase("csv")) {
				writer = new CSVSaver();
				extend = ".csv";
			}
			else if(output_format.equalsIgnoreCase("arff")) {
				writer = new ArffSaver();
				extend = ".arff";
			}
			else{
				System.out.println("No data format, specify .csv or .arff format");
				return;
			}
		}
		
		for(int i=1; i<=folds_count; i++){
			filter.setFold(i);
			System.out.println("Fold "+i);
			
			// prepare and apply filter for training data here
			filter.setInvertSelection(true);   // invert the selection to get other data 
			filter.setInputFormat(data);       // prepare the filter for the data format
			Instances train_instances = Filter.useFilter(data, filter);
			String train_fileName = Paths.get(directory, name + String.format("_train_%02d", i) + extend).toString();
			writer.setInstances(train_instances);
			writer.setFile(new File(train_fileName));
			writer.writeBatch();
			
			    
			// apply filter for test data here
			filter.setInvertSelection(false);  // do not invert the selection
			filter.setInputFormat(data);       // prepare the filter for the data format
			Instances test_instances = Filter.useFilter(data, filter);
			String test_fileName = Paths.get(directory, name + String.format("_test_%02d", i) + extend).toString();
			writer.setInstances(test_instances);
			writer.setFile(new File(test_fileName));
			writer.writeBatch();
			
			System.out.println(String.format("Training instances# + Testing instances# = %d + %d = %d",
					train_instances.size(), test_instances.size(), train_instances.size() + test_instances.size()));
			System.out.println();
		}
	}
}
