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
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

public class CannonAlgorithm extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(CannonAlgorithm.class);

    /**
     * P 16
     * A_ROW 10000
     *
     * P 36
     * A_ROW 10002
     *
     * P 81
     * A_ROW 10008
     *
     * P 100
     * A_ROW 10000
     *
     * P 1000
     * A_ROW 10016
     */

    private static final Integer P = 81; // number of regions
    private static final Integer A_ROW = 10008;
    private static final Integer A_COL = A_ROW;
    private static final Integer B_ROW = A_ROW;
    private static final Integer B_COL = A_ROW;

    private static final Integer SQRT_P = (int)Math.sqrt(P);

    private static final String PHASE_2_ITERATION_NUM = "PHASE_2_ITERATION_NUM";

    private static Pair<Integer, Integer> getBlockCoordinatesForData(SparseEncoding data) {
        // find i,j coordinates for block
        int i,j;
        if (data.getSrc() == 'A') {
            i = data.getRow() / (A_ROW / (int) Math.sqrt(P));
            j = data.getCol() / (A_COL / (int) Math.sqrt(P));

        } else if (data.getSrc() == 'B') {
            i = data.getRow() / (B_ROW / (int) Math.sqrt(P));
            j = data.getCol() / (B_COL / (int) Math.sqrt(P));

        } else {
            // src == 'C'
            i = data.getRow() / (A_ROW / (int) Math.sqrt(P));
            j = data.getCol() / (B_COL / (int) Math.sqrt(P));
        }
        return new Pair<>(i, j);
    }

    private static Pair<Integer, Integer> getInitialAlignmentOfBlock(Pair<Integer, Integer> blockCoordinates,
                                                                     SparseEncoding data) {
        int block_i = blockCoordinates.getKey();
        int block_j = blockCoordinates.getValue();
        int i,j;

        if (data.getSrc() == 'A') {
            i = block_i;
            j = (block_j - block_i + SQRT_P) % SQRT_P;
        } else if (data.getSrc() == 'B') {
            i = (block_i - block_j + SQRT_P) % SQRT_P;
            j = block_j;
        } else {
            // src == 'C'
            i = block_i;
            j = block_j;
        }
        return new Pair<>(i, j);
    }

    private static Pair<Integer, Integer> rollBlockCoordinates(Pair<Integer, Integer> blockCoordinates,
                                                               SparseEncoding data, int iter) {
        int block_i = blockCoordinates.getKey();
        int block_j = blockCoordinates.getValue();
        int i,j;

        if (data.getSrc() == 'A') {
            i = block_i;
            j = Math.floorMod(block_j - iter,  SQRT_P);
        } else if (data.getSrc() == 'B') {
            i = Math.floorMod(block_i - iter,  SQRT_P);
            j = block_j;
        } else {
            // src == 'C'
            i = block_i;
            j = block_j;
        }
        return new Pair<>(i, j);
    }

    public static class CannonFirstPhaseMapper extends Mapper<Object, Text, CannonKey, SparseEncoding> {

        @Override
        public void map(final Object obj, final Text value, final Context context) throws IOException, InterruptedException {
            SparseEncoding data = new SparseEncoding(value.toString());

            Pair<Integer, Integer> blockCoordinates = getBlockCoordinatesForData(data);
            Pair<Integer, Integer> partitionCoordinates = getInitialAlignmentOfBlock(blockCoordinates, data);
            logger.debug("Src: " + data.getSrc() + "; Block key: " + blockCoordinates.toString() +
                    " goes to partition: " + partitionCoordinates.toString());

            context.write(new CannonKey(partitionCoordinates), data);
        }
    }

    public static class CannonSecondPhaseMapper extends Mapper<Object, Text, CannonKey, SparseEncoding> {

        @Override
        public void map(final Object obj, final Text value, final Context context) throws IOException, InterruptedException {
            SparseEncoding data = new SparseEncoding(value.toString());

            Pair<Integer, Integer> blockCoordinates = getBlockCoordinatesForData(data);
            // need this for getting original alignment
            blockCoordinates = getInitialAlignmentOfBlock(blockCoordinates, data);

            int iterationNum = Integer.parseInt(context.getConfiguration().get(PHASE_2_ITERATION_NUM));
            Pair<Integer, Integer> partitionCoordinates = rollBlockCoordinates(blockCoordinates, data, iterationNum);

            context.write(new CannonKey(partitionCoordinates), data);

        }
    }

    public static class CannonAggregationMapper extends Mapper<Object, Text, CannonKey, IntWritable> {

        @Override
        public void map(final Object obj, final Text value, final Context context) throws IOException, InterruptedException {
            SparseEncoding data = new SparseEncoding(value.toString());
            context.write(new CannonKey(data.getRow(), data.getCol()), new IntWritable(data.getVal()));

        }
    }

    public static class CannonAggregationReducer extends Reducer<CannonKey, IntWritable, CannonKey, IntWritable> {
        @Override
        public void reduce(final CannonKey key, final Iterable<IntWritable> values, final Context context)
                throws IOException, InterruptedException {
            context.write(key, new IntWritable(
                    StreamSupport.stream(values.spliterator(), false).mapToInt(IntWritable::get).sum()
            ));
        }
    }

    /**
     * Receives 2 sub-matrices, say A_ij and B_ij
     * C_ij = (sequential) matrix multiplication of A_ij and B_ij sub-matrices
     *
     * Using Hashmaps for SPARSE OPTIMIZATION!!!!!!
     * Ain't no one got time to store them stupid ass zeros -\_(-.-)_-
     */
    public static class CannonReducer extends Reducer<CannonKey, SparseEncoding, SparseEncoding, NullWritable> {
        // TODO: do we need a grouping comparator??

        private static Pair<Integer, Integer> reduceDataCoordinatesTo00(SparseEncoding data) {
            // reduce coordinates of data to base of 0,0
            Pair<Integer, Integer> pair;
            int i, j, val_per_row, val_per_col;

            if (data.getSrc() == 'A') {
                // reduce coordinates of data to base of 0,0
                val_per_row = A_ROW / (int) Math.sqrt(P);
                val_per_col = A_COL / (int) Math.sqrt(P);
                i = data.getRow() % val_per_row;
                j = data.getCol() % val_per_col;
            } else if (data.getSrc() == 'B') {
                // reduce coordinates of data to base of 0,0
                val_per_row = B_ROW / (int) Math.sqrt(P);
                val_per_col = B_COL / (int) Math.sqrt(P);
                i = data.getRow() % val_per_row;
                j = data.getCol() % val_per_col;
            } else {
                // src == 'C'
                val_per_row = A_ROW / (int) Math.sqrt(P);
                val_per_col = B_COL / (int) Math.sqrt(P);
                i = data.getRow() % val_per_row;
                j = data.getCol() % val_per_col;
            }

            return new Pair<>(i, j);

        }

        private static Pair<Integer, Integer> blowupKey_C(CannonKey key, Pair<Integer, Integer> p) {
            // blow up reduced coordinates to match final coordinates of resultant block

            int val_per_row, val_per_col;

            val_per_row = A_ROW / (int) Math.sqrt(P);
            val_per_col = B_COL / (int) Math.sqrt(P);

            // blow up reduced coordinates to match final coordinates of resultant block
            return new Pair<>((key.getPartitionRow() * val_per_row) + p.getKey(), (key.getPartitionCol() * val_per_col) + p.getValue());
        }

        @Override
        public void reduce(final CannonKey partitionKey, final Iterable<SparseEncoding> values, final Context context)
                throws IOException, InterruptedException {

            Map<Pair<Integer, Integer>, Integer> A_map = new HashMap<>();
            Map<Pair<Integer, Integer>, Integer> B_map = new HashMap<>();
            Map<Pair<Integer, Integer>, Integer> C_map = new HashMap<>();

            for (SparseEncoding data : values) {
                if (data.getSrc() == 'A') {
                    A_map.put(reduceDataCoordinatesTo00(data), data.getVal());
                } else if (data.getSrc() == 'B') {
                    B_map.put(reduceDataCoordinatesTo00(data), data.getVal());
                } else {
                    // src == 'C'
                    C_map.put(reduceDataCoordinatesTo00(data), data.getVal());
                }
            }

            int val_per_row = A_ROW / SQRT_P;
            int val_per_col = B_COL / SQRT_P;
            int val;
            Pair<Integer, Integer> pij, pkj, pik, p_ij, blownUpDataCoordinates;

            // multiply A and B
            for (int i = 0; i < val_per_row; i++) {

                // this is wrong, but assuming square sub-matrices it's FINE - should look at rows of B
                for (int k = 0; k < val_per_col; k++) {
                    pik = new Pair<>(i, k);
                    if (A_map.getOrDefault(pik, 0) == 0) {
                        continue;
                    }

                    // this is wrong, but assuming square sub-matrices it's FINE - should look at cols of B
                    for (int j = 0; j < val_per_col; j++) {
                        pij = new Pair<>(i, j);
                        pkj = new Pair<>(k, j);

                        if (B_map.getOrDefault(pkj, 0) == 0) {
                            continue;
                        }

                        val = C_map.getOrDefault(pij, 0) + (
                                A_map.getOrDefault(pik, 0) * B_map.getOrDefault(pkj, 0));
                        if (val != 0) {
                            C_map.put(pij, val);
                        }

                    }
                }

                for(int l = 0; l < val_per_col; l++) {
                    // emit immediately
                    p_ij = new Pair<>(i,l);
                    blownUpDataCoordinates = blowupKey_C(partitionKey, p_ij);
                    if ((val = C_map.getOrDefault(p_ij, 0)) != 0) {
                        context.write(
                                new SparseEncoding(blownUpDataCoordinates.getKey(), blownUpDataCoordinates.getValue(),
                                        val, 'C'), null
                        );
                    }
                }

            }

        }
    }

//    /**
//     * Custom partitioner class
//     */
//    public static class CannonKeyPartitioner extends Partitioner<CannonKey, SparseEncoding> {
//        @Override
//        public int getPartition(CannonKey key, SparseEncoding value, int numPartitions) {
//            int partitionId = (((int) Math.sqrt(P)) * key.getPartitionRow())  + key.getPartitionCol();
//            logger.debug(
//                    "key: " + key.getPartitionRow() + "," + key.getPartitionCol() +
//                            " value: " + value.toString() +
//                            " partitionid: " + partitionId +
//                            " hashPartition: " + Math.floorMod(key.hashCode(), numPartitions)
//            );
//            return partitionId;
//        }
//    }

    private boolean runPhase1(String[] inputPaths, String outputPath)
            throws IOException, ClassNotFoundException, InterruptedException {

        final Configuration conf = getConf();

        final Job job = Job.getInstance(conf, this.getClass().getName());
        job.setJarByClass(this.getClass());
        final Configuration jobConf = job.getConfiguration();

        jobConf.set("mapreduce.output.textoutputformat.separator", "");

        FileInputFormat.addInputPaths(job, String.join(",", inputPaths));

        job.setMapOutputKeyClass(CannonKey.class);
        job.setMapOutputValueClass(SparseEncoding.class);

        job.setMapperClass(CannonFirstPhaseMapper.class);
        job.setReducerClass(CannonReducer.class);

//        job.setPartitionerClass(CannonKeyPartitioner.class);

        job.setNumReduceTasks(P);
        jobConf.set("mapred.task.timeout", "3600000000");

        job.setOutputKeyClass(SparseEncoding.class);
        job.setOutputValueClass(NullWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true);
    }

    private boolean runPhase2(String[] inputPaths, String outputPath, int iter)
            throws IOException, ClassNotFoundException, InterruptedException {

        final Configuration conf = getConf();

        final Job job = Job.getInstance(conf, this.getClass().getName());
        job.setJarByClass(this.getClass());
        final Configuration jobConf = job.getConfiguration();

        jobConf.set("mapreduce.output.textoutputformat.separator", "");

        FileInputFormat.addInputPaths(job, String.join(",", inputPaths));

        job.setMapOutputKeyClass(CannonKey.class);
        job.setMapOutputValueClass(SparseEncoding.class);

        job.setMapperClass(CannonSecondPhaseMapper.class);
        job.setReducerClass(CannonReducer.class);

//        job.setPartitionerClass(CannonKeyPartitioner.class);

        job.setNumReduceTasks(P);
        jobConf.set("mapred.task.timeout", "3600000000");

        job.setOutputKeyClass(SparseEncoding.class);
        job.setOutputValueClass(NullWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        job.getConfiguration().set(PHASE_2_ITERATION_NUM, String.valueOf(iter));

        return job.waitForCompletion(true);
    }

    private boolean runAggregationPhase(String[] inputPaths, String outputPath)
            throws IOException, ClassNotFoundException, InterruptedException {

        final Configuration conf = getConf();

        final Job job = Job.getInstance(conf, this.getClass().getName());
        job.setJarByClass(this.getClass());
        final Configuration jobConf = job.getConfiguration();

        jobConf.set("mapreduce.output.textoutputformat.separator", ",");

        FileInputFormat.addInputPaths(job, String.join(",", inputPaths));

        job.setMapOutputKeyClass(CannonKey.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setMapperClass(CannonAggregationMapper.class);
        job.setCombinerClass(CannonAggregationReducer.class);
        job.setReducerClass(CannonAggregationReducer.class);

//        job.setPartitionerClass(CannonKeyPartitioner.class);

//        job.setNumReduceTasks(P);
//        jobConf.set("mapred.task.timeout", "3600000000");

        job.setOutputKeyClass(CannonKey.class);
        job.setOutputValueClass(IntWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true);
    }

    @Override
    public int run(final String[] args) throws Exception {

        logger.setLevel(Level.INFO);

        List<String> aggregationInputs = new ArrayList<>();
        aggregationInputs.add(args[1]+"_"+0+"/part-r*");
        if(! runPhase1(new String[] {args[0]},  args[1]+"_"+0)) {
            return 1;
        }

        for (int iter =  1; iter <= SQRT_P-1; iter++) {
            aggregationInputs.add(args[1]+"_"+iter+"/part-r*");
            if(! runPhase2(new String[] {args[0]},  args[1]+"_"+iter, iter)) { // , args[1]+"_"+(iter-1)
                return 1;
            }
        }

        if(! runAggregationPhase(aggregationInputs.toArray(new String[aggregationInputs.size()]),  args[1]+"-aggregation")) {
            return 1;
        }

        return 0;
    }
}