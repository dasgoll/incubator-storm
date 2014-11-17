package org.apache.storm.hdfs.trident;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.common.rotation.MoveFileAction;
import org.apache.storm.hdfs.common.security.HdfsSecurityUtil;
import org.apache.storm.hdfs.trident.format.*;
import org.apache.storm.hdfs.trident.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.trident.rotation.FileSizeRotationPolicy;

import storm.trident.Stream;
import storm.trident.TridentState;
import storm.trident.TridentTopology;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.state.StateFactory;
import storm.trident.tuple.TridentTuple;

public class TridentFileTopology {

    public static StormTopology buildTopology(String hdfsUrl){
        FixedBatchSpout spout = new FixedBatchSpout(new Fields("sentence", "key"), 1000, new Values("the cow jumped over the moon", 1l),
                new Values("the man went to the store and bought some candy", 2l), new Values("four score and seven years ago", 3l),
                new Values("how many apples can you eat", 4l), new Values("to be or not to be the person", 5l));
        spout.setCycle(true);

        TridentTopology topology = new TridentTopology();
        Stream stream = topology.newStream("spout1", spout);

        Fields hdfsFields = new Fields("sentence", "key");

        FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                .withPath("/trident")
                .withPrefix("trident")
                .withExtension(".txt");

        RecordFormat recordFormat = new DelimitedRecordFormat()
                .withFields(hdfsFields);

        FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, FileSizeRotationPolicy.Units.MB);

        HdfsState.Options options = new HdfsState.HdfsFileOptions()
                .withFileNameFormat(fileNameFormat)
                .withRecordFormat(recordFormat)
                .withRotationPolicy(rotationPolicy)
                .withFsUrl(hdfsUrl);

        StateFactory factory = new HdfsStateFactory().withOptions(options);

        TridentState state = stream
                .partitionPersist(factory, hdfsFields, new HdfsUpdater(), new Fields());

        return topology.build();
    }

    public static void main(String[] args) throws Exception {
        Config conf = new Config();
        conf.setMaxSpoutPending(5);
        if (args.length == 1) {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("wordCounter", conf, buildTopology(args[0]));
            Thread.sleep(120 * 1000);
        } else if(args.length == 2) {
            conf.setNumWorkers(3);
            StormSubmitter.submitTopology(args[1], conf, buildTopology(args[0]));
        } else if (args.length == 4) {
            System.out.println("hdfs url: " + args[0] + ", keytab file: " + args[2] + 
                ", principal name: " + args[3] + ", toplogy name: " + args[1]);
            conf.put(HdfsSecurityUtil.STORM_KEYTAB_FILE_KEY, args[2]);
            conf.put(HdfsSecurityUtil.STORM_USER_NAME_KEY, args[3]);
            conf.setNumWorkers(3);
            StormSubmitter.submitTopology(args[1], conf, buildTopology(args[0]));
        } else {
            System.out.println("Usage: TridentFileTopology <hdfs url> [topology name] [keytab file] [principal name]");
        }
    }
}