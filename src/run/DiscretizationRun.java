/*
 * @author Van Quoc Phuong Huynh, FAW JKU
 *
 */

package run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import prepr.DataDiscretizer;

/**
 * Discretize all pairs of train-test data sets in a directory
 *
 */
public class DiscretizationRun {
	private static String directory = "data/inputs/german_arff";
	private static boolean one_outputfile = true;
	
	public static void main(String[] args) throws Exception{
		if(args.length < 2){
			System.out.println("Parameters: <data directory> <one_outputfile>");
			//discretize_all(directory, one_outputfile);	// uncomment for debugging run
			return;
		}
		directory = args[0];
		one_outputfile = Boolean.parseBoolean(args[1]);
		discretize_all(directory, one_outputfile);
	}
	
	/**
	 * Discretize pairs of files (train and test files must contain "train" and "test" in the file names) in a directory.
	 * @param data_dir_path directory containing pairs of train and test files in arff format
	 * @param one_outputfile one csv output file or two separately discretized files of each pair of train and test files
	 * @throws IOException
	 */
	public static void discretize_all(String data_dir_path, boolean one_outputfile) throws IOException{
	    File data_dir = new File(data_dir_path);
	    File[] train_files = data_dir.listFiles(new FilenameFilter() {
												        @Override
												        public boolean accept(File dir, String name) {
												            return name.toLowerCase().contains("train");
												        }
												    });
	    Arrays.sort(train_files);
	    
	    File[] test_files = data_dir.listFiles(new FilenameFilter() {
												        @Override
												        public boolean accept(File dir, String name) {
												            return name.toLowerCase().contains("test");
												        }
												    });
	    Arrays.sort(test_files);
	    
	    if(train_files.length != test_files.length) return;
	    
	    long total_time = 0;
	    for(int i=0; i<train_files.length; i++){
	    	System.out.println("Discretize fold " + i);
	    	total_time += discrete(train_files[i], test_files[i], i, one_outputfile);
	    	System.out.println("---------------------------------------------------------");
	    }
	    System.out.println("Average running time: " + (total_time/test_files.length) + " ms");
	}
	
	private static long discrete(File train_filename, File test_filename, int fold_index, boolean one_outputfile) throws IOException {
		System.out.println("Training filename: " + train_filename.getAbsolutePath());
    	System.out.println("Testing filename: " + test_filename.getAbsolutePath());
    	
    	String output_filename = Paths.get(train_filename.getParent(), String.format("fold_%02d", fold_index) + ".csv").toString();    	
    	String train_output_filename = Paths.get(train_filename.getParent(), String.format("train_labeled_%02d", fold_index) + ".csv").toString();
    	String test_output_filename = Paths.get(train_filename.getParent(), String.format("test_labeled_%02d", fold_index) + ".csv").toString();    	
	    
	    DataDiscretizer dd = new DataDiscretizer();
	    long run_time = dd.learn_discrete_values(train_filename.getAbsolutePath());
	    
	    int train_count, test_count;
	    if(one_outputfile){
	    	System.out.println("Output filename: " + output_filename);
	    	train_count = dd.discretize(train_filename.getAbsolutePath(), output_filename);
		    test_count = dd.discretize(test_filename.getAbsolutePath(), test_output_filename);
		    append(output_filename, test_output_filename);
		    new File(test_output_filename).delete();
	    }else{
	    	System.out.println("Train output filename: " + train_output_filename);
	    	System.out.println("Test output filename: " + test_output_filename);
	    	train_count = dd.discretize(train_filename.getAbsolutePath(), train_output_filename);
	    	test_count = dd.discretize(test_filename.getAbsolutePath(), test_output_filename);
	    }
	    
	    System.out.println("Train example count: " + train_count);
	    System.out.println("Test example count: " + test_count);
	    System.out.println("Running time: " + run_time + " ms");
	    
	    return run_time;
	}
	
	private static void append(String filename1, String filename2) throws IOException{
		// open file in append mode
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename1, true));
		BufferedReader reader = new BufferedReader(new FileReader(filename2));
		
		String line;
		reader.readLine();	// pass the csv head line 
		while ((line = reader.readLine()) != null) {
			writer.write(line);
			writer.newLine();
		}
		
		reader.close();
		writer.flush();
		writer.close();
	}
}
