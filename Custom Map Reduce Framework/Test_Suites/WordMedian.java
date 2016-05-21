/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import custom.mr.*;


public class WordMedian
{
  private static double median = 0;
  private final static IntWritable ONE = new IntWritable(1);
  /**
   * Maps words from line of text into a key-value pair; the length of the word
   * as the key, and 1 as the value.
   */
  public static class WordMedianMapper extends
      Mapper<Object, Text, IntWritable, IntWritable> 
  {
    private Text length = new Text();
    /**
     * Emits a key-value pair for counting the word. Outputs are (IntWritable,
     * IntWritable).
     * 
     * @param value
     *          This will be a line of text coming in from our input file.
     */
    public void map(Object key, Text value, Context context)
        throws IOException, InterruptedException 
    {
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) 
      {
        String string = itr.nextToken();
        length.set(string.length() + "");
        context.write(length, ONE);
      }
    }
  }
  /**
   * Performs integer summation of all the values for each key.
   */
  public static class WordMedianReducer extends
      Reducer<Text, IntWritable, Text, IntWritable> 
  {
    private IntWritable val = new IntWritable();
    /**
     * Sums all the individual values within the iterator and writes them to the
     * same key.
     * 
     * @param key
     *          This will be a length of a word that was read.
     * @param values
     *          This will be an iterator of all the values associated with that
     *          key.
     */
    public void reduce(Text key, Iterable<IntWritable> values,
        Context context) throws IOException, InterruptedException 
    {
      int sum = 0;
      for (IntWritable value : values) 
      {
        sum += value.get();
      }
      val.set(sum);
      context.write(key, val);
    }
  }
  /**
   * This is a standard program to read and find a median value based on a file
   * of word counts such as: 1 456, 2 132, 3 56... Where the first values are
   * the word lengths and the following values are the number of times that
   * words of that length appear.
   * 
   * @param path
   *          The path to read the HDFS file from (part-r-00000...00001...etc).
   * @param medianIndex1
   *          The first length value to look for.
   * @param medianIndex2
   *          The second length value to look for (will be the same as the first
   *          if there are an even number of words total).
   * @throws IOException
   *           If file cannot be found, we throw an exception.
   * */
  private static double readAndFindMedian(String path, int medianIndex1,
      int medianIndex2, Configuration conf) throws IOException 
  {
    //FileSystem fs = FileSystem.get(conf);
    //Path file = new Path(path, "part-r-00000");
	File outputfolder = new File(path + "part-0");
	
    BufferedReader br = null;
    
    try 
    {
    	System.out.println("Output folder: " + outputfolder);
      br = new BufferedReader(new FileReader(outputfolder.listFiles()[0]));
      int num = 0;
      String line;
      while ((line = br.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line);
        // grab length
        String currLen = st.nextToken();
        // grab count
        String lengthFreq = st.nextToken();
        int prevNum = num;
        num += Integer.parseInt(lengthFreq);
        if (medianIndex2 >= prevNum && medianIndex1 <= num) {
          System.out.println("The median is: " + currLen);
          br.close();
          return Double.parseDouble(currLen);
        } else if (medianIndex2 >= prevNum && medianIndex1 < num) {
          String nextCurrLen = st.nextToken();
          double theMedian = (Integer.parseInt(currLen) + Integer
              .parseInt(nextCurrLen)) / 2.0;
          System.out.println("The median is: " + theMedian);
          br.close();
          return theMedian;
        }
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
    // error, no median found
    return -1;
  }
  public static void main(String[] args) throws Exception 
  {
    
    //setConf(new Configuration());
    Configuration conf = new Configuration();
    @SuppressWarnings("deprecation")
    Job job = Job.getInstance(conf, "word median");
    job.setJarByClass(WordMedian.class);
    job.setMapperClass(WordMedianMapper.class);
    job.setCombinerClass(WordMedianReducer.class);
    job.setReducerClass(WordMedianReducer.class);
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(IntWritable.class);
    job.setNumReducerTasks(1); 								// So that readAndFind Median reads only one part file at a time
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    boolean result = job.waitForCompletion(true);
    // Wait for JOB 1 -- get middle value to check for Median
    long totalWords = job.getMapperOutputCounter();
    int medianIndex1 = (int) Math.ceil((totalWords / 2.0));
    int medianIndex2 = (int) Math.floor((totalWords / 2.0));
    
    String partFilePath = "/tmp/output/";
    if(!args[1].startsWith("s3:"))
    {
    	// If on pseudo, fetch from local path as is.
    	partFilePath = args[1];
    }
    median = readAndFindMedian(partFilePath, medianIndex1, medianIndex2, conf);
    System.out.println("Median is " + median);
  }
  public double getMedian() 
  {
    return median;
  }
}