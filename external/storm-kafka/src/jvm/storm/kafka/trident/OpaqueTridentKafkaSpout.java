/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package storm.kafka.trident;

import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import storm.kafka.spout.KafkaConfig;
import storm.kafka.spout.partition.GlobalPartitionInformation;
import storm.kafka.spout.partition.Partition;
import storm.trident.spout.IOpaquePartitionedTridentSpout;

import java.util.Map;
import java.util.UUID;


public class OpaqueTridentKafkaSpout implements IOpaquePartitionedTridentSpout<GlobalPartitionInformation, Partition, Map> {


    private KafkaConfig _config;
    private String _topologyInstanceId = UUID.randomUUID().toString();
    private storm.kafka.trident.Coordinator coordinator;

    public OpaqueTridentKafkaSpout(KafkaConfig config) {
        _config = config;
    }

    public OpaqueTridentKafkaSpout withCoordinator(storm.kafka.trident.Coordinator coordinator) {
        this.coordinator = coordinator;
        return this;
    }

    @Override
    public IOpaquePartitionedTridentSpout.Emitter<GlobalPartitionInformation, Partition, Map> getEmitter(Map conf, TopologyContext context) {
        return new TridentKafkaEmitter(conf, context, _config, _topologyInstanceId).asOpaqueEmitter();
    }

    @Override
    public IOpaquePartitionedTridentSpout.Coordinator getCoordinator(Map conf, TopologyContext tc) {
        if(this.coordinator == null) {
            this.coordinator = new storm.kafka.trident.Coordinator(conf, _config);
        }
        return this.coordinator;
    }

    @Override
    public Fields getOutputFields() {
        return _config.scheme.getOutputFields();
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }

}
