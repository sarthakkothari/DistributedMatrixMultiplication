package meanpartitions;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BBAlgorithm extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(BBAlgorithm.class);

    private static final Integer A_ROW = 10000;
    private static final Integer A_COL = 10000;
    private static final Integer B_ROW = 10000;
    private static final Integer B_COL = 10000;
    private static final Integer P_ROW = 5; // 4 4 9 5
    private static final Integer P_MID = 5; // 2 3 3 5
    private static final Integer P_COL = 4; // 2 3 3 4

    // Get (row, col) coordinates for each of the blocks
    private static Pair<Integer, Integer> getBlockCoordinates(SparseEncoding data) {
        int i, j;
        if (data.getSrc() == 'A') {
            i = data.getRow() / (A_ROW / P_ROW);
            j = data.getCol() / (A_COL / P_MID);
        } else {
            i = data.getRow() / (B_ROW / P_MID);
            j = data.getCol() / (B_COL / P_COL);
        }
        return new Pair<>(i, j);
    }


    public static class MultiplicationMapper extends Mapper<Object, Text, IntWritable, SparseEncoding> {
        private final IntWritable key = new IntWritable();

        @Override
        public void map(final Object obj, final Text value, final Context context) throws IOException, InterruptedException {
            SparseEncoding data = new SparseEncoding(value.toString());
            Pair<Integer, Integer> blockCoordinates = getBlockCoordinates(data);
            if (data.getSrc() == 'A') {
                for (Integer partition : alignBlocksForA(blockCoordinates)) {
                    key.set(partition);
                    context.write(key, data);
                }
            } else {
                for (Integer partition : alignBlocksForB(blockCoordinates)) {
                    key.set(partition);
                    context.write(key, data);
                }
            }
        }

        // Need this to send blocks of Matrix A to the respective processors
        private List<Integer> alignBlocksForA(Pair<Integer, Integer> blockCoordinates) {
            List<Integer> partitions = new ArrayList<>();
            int base = (blockCoordinates.getKey() * P_COL) + ((P_ROW * P_COL) * blockCoordinates.getValue());
            for (int i = 0; i < P_COL; i++) {
                partitions.add(base + i);
            }
            return partitions;
        }

        // Need this to send blocks of Matrix B to the respective processors
        private List<Integer> alignBlocksForB(Pair<Integer, Integer> blockCoordinates) {
            List<Integer> partitions = new ArrayList<>();
            int base = (P_ROW * P_COL * blockCoordinates.getKey()) + blockCoordinates.getValue();
            for (int i = 0; i < P_ROW; i++) {
                partitions.add(base + (P_COL*i));
            }
            return partitions;
        }
    }


    public static class MultiplicationReducer extends Reducer<IntWritable, SparseEncoding, SparseEncoding, NullWritable> {
        private static List<SparseEncoding> listA = new ArrayList<>();
        private static Map<Integer, List<SparseEncoding>> mapB = new HashMap<>();

        @Override
        public void reduce(IntWritable key, Iterable<SparseEncoding> values, Context context) throws IOException, InterruptedException {
            listA.clear();
            mapB.clear();

            for (SparseEncoding value : values) {
                SparseEncoding data = new SparseEncoding(value);
                if (data.getSrc() == 'A') {
                    listA.add(data);
                } else {
                    Integer k = data.getRow();
                    if (mapB.containsKey(k)) {
                        mapB.get(k).add(data);
                    } else {
                        List<SparseEncoding> temp = new ArrayList<>();
                        temp.add(data);
                        mapB.put(k, temp);
                    }
                }
            }

            for (SparseEncoding cellA : listA) {
                List<SparseEncoding> listB = mapB.getOrDefault(cellA.getCol(), Collections.emptyList());
                for (SparseEncoding cellB : listB) {
                    SparseEncoding cellC = new SparseEncoding(cellA.getRow(), cellB.getCol(), cellA.getVal() * cellB.getVal());
                    context.write(cellC, null);
                }
            }
        }
    }

    public static class AggregationMapper extends Mapper<Object, Text, SimpleKey, IntWritable> {
        private final SimpleKey key = new SimpleKey();
        private final IntWritable val = new IntWritable();

        @Override
        public void map(final Object obj, final Text value, final Context context) throws IOException, InterruptedException {
            SparseEncoding data = new SparseEncoding(value.toString());
            key.setRow(data.getRow());
            key.setCol(data.getCol());
            val.set(data.getVal());
            context.write(key, val);
        }
    }


    public static class AggregationReducer extends Reducer<SimpleKey, IntWritable, SimpleKey, IntWritable> {
        private final IntWritable val = new IntWritable();

        @Override
        public void reduce(SimpleKey key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            val.set(sum);
            context.write(key, val);
        }
    }

    @Override
    public int run(final String[] args) throws Exception {
        final Configuration conf = new Configuration();
        final Job job1 = Job.getInstance(conf, "BB-Matrix Multiplication");
        job1.setJarByClass(BBAlgorithm.class);
        final Configuration jobConf1 = job1.getConfiguration();
        jobConf1.set("mapreduce.output.textoutputformat.separator", ",");
        jobConf1.set("mapreduce.task.timeout", "1800000000");
//        Delete output directory, only to ease local development; will not work on AWS. =======    ====
//        final FileSystem fileSystem = FileSystem.get(conf);
//        if (fileSystem.exists(new Path(args[1]))) {
//            fileSystem.delete(new Path(args[1]), true);
//        }
//        if (fileSystem.exists(new Path(args[2]))) {
//            fileSystem.delete(new Path(args[2]), true);
//        }
//        ================
        job1.setMapperClass(MultiplicationMapper.class);
        job1.setReducerClass(MultiplicationReducer.class);

        job1.setMapOutputKeyClass(IntWritable.class);
        job1.setMapOutputValueClass(SparseEncoding.class);

        job1.setOutputKeyClass(SparseEncoding.class);
        job1.setOutputValueClass(NullWritable.class);

        FileInputFormat.addInputPath(job1, new Path(args[0]));
        FileOutputFormat.setOutputPath(job1, new Path(args[1]));

        if (!job1.waitForCompletion(true)) {
            System.exit(1);
        }

        // ===============================================================
        final Job job2 = Job.getInstance(conf, "BB-Matrix Aggregation");
        job2.setJarByClass(BBAlgorithm.class);
        final Configuration jobConf2 = job2.getConfiguration();
        jobConf2.set("mapreduce.output.textoutputformat.separator", ",");
        jobConf2.set("mapreduce.task.timeout", "1800000000");

        job2.setMapperClass(AggregationMapper.class);
        job2.setCombinerClass(AggregationReducer.class);
        job2.setReducerClass(AggregationReducer.class);

        job2.setMapOutputKeyClass(SimpleKey.class);
        job2.setMapOutputValueClass(IntWritable.class);

        job2.setOutputKeyClass(SimpleKey.class);
        job2.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job2, new Path(args[1]));
        FileOutputFormat.setOutputPath(job2, new Path(args[2]));

        if (!job2.waitForCompletion(true)) {
            System.exit(1);
        }

        return 0;
    }
}
