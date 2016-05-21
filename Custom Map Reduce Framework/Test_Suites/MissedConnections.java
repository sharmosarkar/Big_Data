import java.io.IOException;
import java.util.ArrayList;

import custom.mr.*;


/**
 * Missed Connection : Calculates Missed Connections between 2 hop paths
 * 
 * @author Ashish Kalbhor, Yogiraj Awati
 *
 */
public class MissedConnections {
	static String SEPERATOR = ",";

	public static void main(String[] args) throws Exception {

		Configuration config = new Configuration();
		if (args.length != 2) {
			System.err.println("Usage: Missed Connections <input> <output>");
			System.exit(4);
		}

		Job job = Job.getInstance(config, "Missed Connections");
		job.setJarByClass(MissedConnections.class);
		job.setMapperClass(ConnectionMapper.class);
		job.setReducerClass(ConnectionReducer.class);
		job.setNumReducerTasks(1);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);

	}

	//Author : Ashish
	// Mapper
	public static class ConnectionMapper extends Mapper<Object, Text, Text, Text> {

		public void map(Object offset, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString();
			String newLine = line.replaceAll("\"", "");
			String correctLine = newLine.replaceAll(", ", ":");
			String[] record = correctLine.split(",");
			Text carrierName = new Text();
			Text year = new Text();
			
			Text originId = new Text();
			Text destinationId = new Text();
			Text arrivalTime = new Text();
			Text scheduledArrivalTime = new Text();
			Text deptTime = new Text();
			Text scheduleddeptTime = new Text();
			Text date = new Text();
			Text cancellation = new Text();
			int y;
			// isRecordValid : checks if the required fields are not null
			try {
				if (record.length > 0 && isRecordValid(record) ) {
					//extract parameters for the calculations
					y = Integer.parseInt(record[0]);
					carrierName.set(record[6]); 
					year.set(record[0]); 
					originId.set(record[11]);
					destinationId.set(record[20]);
					date.set(record[5]);
					arrivalTime.set(record[41]);
					scheduledArrivalTime.set(record[40]);
					deptTime.set(record[30]);
					scheduleddeptTime.set(record[29]);
					cancellation.set(record[47]);
								
					//Origin and destination key
					String originKey = carrierName.toString()+SEPERATOR+year.toString()+SEPERATOR+originId.toString()+SEPERATOR+
							date.toString();
					String destKey = carrierName.toString()+SEPERATOR+year.toString()+SEPERATOR+
							destinationId.toString()+SEPERATOR+date.toString();
					
					//Origin and destination Value
					String originValue = "origin"+SEPERATOR + arrivalTime.toString()+SEPERATOR+scheduledArrivalTime.toString()+SEPERATOR+cancellation;
					String destValue = "departure"+SEPERATOR +deptTime.toString()+SEPERATOR+scheduleddeptTime.toString()+SEPERATOR+cancellation;
					
					context.write(new Text(originKey), new Text(originValue));
					context.write(new Text(destKey), new Text(destValue));

				}

			} catch (Exception e) {
			}

		}

		//checks if the required values are null
		private boolean isRecordValid(String[] record) {
			if (record.length < 110 || record[0].isEmpty() || record[6].isEmpty() 
					|| record[11].isEmpty() || record[20].isEmpty() || record[5].isEmpty()
					|| record[41].isEmpty() || record[40].isEmpty() || record[29].isEmpty() || record[30].isEmpty() || record[47].isEmpty())
				return false;
			return true;
		}

	}

	//  Reducer
	// Author: Yogiraj
	public static class ConnectionReducer extends Reducer<Text, Text, Text, Text> {

	
	    
        //origin output : key: carrier name,Year,Origin/Destination Id,date 
	    //Value: "origin/destination"+ arrv time/dept time + sched arrv time/sched dept time+ date+ cancellation
		
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			ArrayList<String> arrivalList = new ArrayList<>();
		    ArrayList<String> deptList = new ArrayList<>();
	        long missedConnection =0 ;
	        long totalConnections = 0;
			String date = null;
			String actualTime = null;
			String year = null;
			String recordType = null;
			String[] data;
			String[] keyData;
			String cancellation = null;
			String airportID = null;
			String scheduledTime=null;
		
			//process key 
			keyData = key.toString().split(SEPERATOR);
			String carrierName= keyData[0];
			year=keyData[1];
			airportID=keyData[2];
			date= keyData[3];
			   
			for (Text value : values) {

				data = value.toString().split(SEPERATOR);
				recordType = data[0]; // origin/destination
			    actualTime = data[1];
			    scheduledTime = data[2];
			    cancellation =data[3];
			    
				if (recordType.equals("origin")) { 
					arrivalList.add(airportID + SEPERATOR+ actualTime + SEPERATOR + scheduledTime + SEPERATOR + date + SEPERATOR + cancellation);
				}
				else if (recordType.equals("departure")) {
					deptList.add(airportID+SEPERATOR+ actualTime + SEPERATOR + scheduledTime + SEPERATOR + date + SEPERATOR + cancellation);
				}
			    	
			}
			/**
			 * A connection is any pair of flight F and G of the same carrier such as F.Destination = G.Origin and the 
			 * scheduled departure of G is <= 6 hours and >= 30 minutes after the scheduled arrival of F.
	           A connection is missed when the actual arrival of F < 30 minutes before the actual departure of G.
			 */
			String arrvId =null;
			String deptId = null;
			String arrvDate = null;
			String deptDate = null;
			String arrSchTime = null;
			String arrActTime = null;
			String depActTime = null;
			String depSchTime = null;
			String arrCan = null;
			String depCan = null;
			String [] arrvData;
			String [] deptData;
		    //Value: "origin/destination"+ arrv time/dept time + sched arrv time/sched dept time+ date+ cancellation+Origin/destination id
			for(String arrvValue : arrivalList)
			{
				arrvData= arrvValue.split(SEPERATOR);
				arrvId = arrvData[0];
				arrActTime = arrvData[1];
				arrSchTime = arrvData[2];
				arrvDate = arrvData[3];
				arrCan = arrvData[4];
				
				// If it is a cancelled arrival, the connecting flight is definitely missed.
				if(arrCan.equals("1"))
				{
					totalConnections++;
					missedConnection++;
					continue;
				}
                //compare each arrival value with all departure values
				for(String deptValue : deptList)
				{
					deptData = deptValue.split(SEPERATOR);
					deptId = deptData[0];
					depActTime=deptData[1];
					depSchTime=deptData[2];
					deptDate=deptData[3];
					if(arrvId.equals(deptId) && arrvDate.equals(deptDate))
					{
						if(checkScheduledTime(arrSchTime, depSchTime))
						{
							totalConnections++;
							if(checkActualTime(arrActTime, depActTime))
							{
								missedConnection++;
							}
														
						}
						
					}
					
				}
				
			}
			
            //output values whose missedconnection>0
			if(missedConnection>0)
				context.write(new Text(carrierName+SEPERATOR+year), new Text(totalConnections+""+SEPERATOR+missedConnection+""));
			
			//Release resources
			arrivalList.clear();
			deptList.clear();
			arrivalList = null;
			deptList = null;
			missedConnection=0;
			
		}
		
	
		//logic for scheduled time
		public boolean checkScheduledTime(String arrTime, String deptTime)
		{
			
			int arrMins;
			int depMins;
			try 
			{
				arrMins = Integer.parseInt(arrTime.substring(0,2)) * 60 + Integer.parseInt(arrTime.substring(2));
				depMins = Integer.parseInt(deptTime.substring(0,2)) * 60 + Integer.parseInt(deptTime.substring(2));
			} catch (NumberFormatException e) 
			{
				return false;
			}
			
			if((depMins >= arrMins + 30) && (depMins <= 360 + arrMins))
				return true;
					
			return false;
		}
		
		//logic for actual time
		public boolean checkActualTime(String arrTime, String deptTime)
		{
			
			int arrMins;
			int depMins;
			try
			{
				arrMins = Integer.parseInt(arrTime.substring(0,2)) * 60 + Integer.parseInt(arrTime.substring(2));
				depMins = Integer.parseInt(deptTime.substring(0,2)) * 60 + Integer.parseInt(deptTime.substring(2));
			} catch (NumberFormatException e) 
			{
				return false;
			}
			
			if((depMins - arrMins) <= 30)
				return true;
					
			return false;
		}
		

	}

}
