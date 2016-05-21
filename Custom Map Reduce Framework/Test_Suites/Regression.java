import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import custom.mr.*;

/**
 * Linear Regression
 * 
 * @author Ashish Kalbhor, Yogiraj Awati
 *
 */
public class Regression {
	static String SEPERATOR = ",";

	public static void main(String[] args) throws Exception {

		Configuration config = new Configuration();
		//setting format of seperator
		//config.set("mapred.textoutputformat.separator", SEPERATOR);
		//String[] arguments = new GenericOptionsParser(config, args).getRemainingArgs();
		if (args.length != 2) {
			System.err.println("Usage: Linear Regression <input> <output>");
			System.exit(4);
		}

		Job job = Job.getInstance(config, "Linear Regression");
		job.setJarByClass(Regression.class);
		job.setMapperClass(RegressionMapper.class);
		job.setReducerClass(RegressionReducer.class);

		job.setNumReducerTasks(1);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);

	}

	// Mapper
	public static class RegressionMapper extends Mapper<Object, Text, Text, Text> {

		public void map(Object offset, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString();
			String newLine = line.replaceAll("\"", "");
			String correctLine = newLine.replaceAll(", ", ":");
			String[] record = correctLine.split(",");
			Text ticketPrice = new Text();
			Text carrierName = new Text();
			Text time = new Text();
			Text year = new Text();
			Text distance = new Text();
			int y;

			// isRecordValid : checks if the required fields are not null

			try {
				if (record.length > 0 && isRecordValid(record)) {
					y = Integer.parseInt(record[0]);
					carrierName.set(record[6]); // coloumn 6
					year.set(record[0]); // coloumn 0
					ticketPrice.set(record[109]); // coloumn 109
					time.set(record[51]); // get time by accessing
														// coloumn 51
					distance.set(record[54]); // get distance - coloumn 54

					// output : key: carrier name , Value: ticketPrice, year, time, distance
					
					String v = ticketPrice.toString() + SEPERATOR + year.toString() + SEPERATOR + time.toString()
							+ SEPERATOR + distance.toString();
					context.write(new Text(carrierName.toString()), new Text(v));

				}

			} catch (Exception e) {
			}

		}

		//checks if the required values are null
		private boolean isRecordValid(String[] record) {
			if (record.length < 110 || record[0].isEmpty() || record[6].isEmpty() || record[51].isEmpty()
					|| record[54].isEmpty() || record[109].isEmpty())
				return false;
			return true;
		}

	}

	// Regression Reducer
	public static class RegressionReducer extends Reducer<Text, Text, Text, Text> {

		HashSet<String> activeIn2015 = new HashSet<String>();

		// input : key: carrier name , Values: list of (ticketPrice, year, time, distance)
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			List<String> cache = new ArrayList<String>();
			String ticketprice = null;
			String distance = null;
			String time = null;
			String year = null;
			String[] data;
			// for each value
			for (Text value : values) 
			{

				data = value.toString().split(SEPERATOR);
				year = data[1];
				ticketprice = data[0];
				distance = data[3];
				time = data[2];
				// for flights active in 2015, populate activeIn2015
				if (year.equals("2015")) 
				{
					activeIn2015.add(key.toString());
				}
				//populate cache which has year : 2010, 2011, 2012, 2013
				if (year.equals("2010") || year.equals("2011") || year.equals("2012") || year.equals("2013")
						|| year.equals("2014")) {
					cache.add(key.toString() + SEPERATOR + ticketprice + SEPERATOR + time + SEPERATOR + distance);
				}

			}
			
			System.out.println("Cache size: " + cache.size());
			System.out.println("Active airline carriers count: " + activeIn2015.size());
			
			//iterate cache and output those values whose key is present in 2015
			for (int i = 0; i < cache.size(); i++) 
			{
				String str = cache.get(i);
				String[] d = str.split(SEPERATOR);
				System.out.println("d => " + d[0]);
				// if the record is present in the 2015 write it to output
				if (activeIn2015.contains(d[0])) 
				{
					System.out.println("Valid record.. printing");
					context.write(new Text(d[0]), new Text(d[1] + SEPERATOR + d[2] + SEPERATOR + d[3]));
				}
			}
			//release resources for next iteration
			cache.clear();

		}

	}

}
