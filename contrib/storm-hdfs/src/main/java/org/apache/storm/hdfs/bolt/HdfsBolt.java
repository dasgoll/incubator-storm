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
package org.apache.storm.hdfs.bolt;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.RotationAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;

public class HdfsBolt extends AbstractHdfsBolt{
    private static final Logger LOG = LoggerFactory.getLogger(HdfsBolt.class);

    public static final String STORM_KEYTAB_FILE_KEY = "storm.keytab.file";
    public static final String STORM_USER_NAME_KEY = "storm.kerberos.principal";

    private FSDataOutputStream out;
    private RecordFormat format;
    private long offset = 0;

    public HdfsBolt withFsUrl(String fsUrl){
        this.fsUrl = fsUrl;
        return this;
    }

    public HdfsBolt withFileNameFormat(FileNameFormat fileNameFormat){
        this.fileNameFormat = fileNameFormat;
        return this;
    }

    public HdfsBolt withRecordFormat(RecordFormat format){
        this.format = format;
        return this;
    }

    public HdfsBolt withSyncPolicy(SyncPolicy syncPolicy){
        this.syncPolicy = syncPolicy;
        return this;
    }

    public HdfsBolt withRotationPolicy(FileRotationPolicy rotationPolicy){
        this.rotationPolicy = rotationPolicy;
        return this;
    }

    public HdfsBolt addRotationAction(RotationAction action){
        this.rotationActions.add(action);
        return this;
    }

    @Override
    public void doPrepare(Map conf, TopologyContext topologyContext, OutputCollector collector) throws IOException {
        LOG.info("Preparing HDFS Bolt...");
        if (UserGroupInformation.isSecurityEnabled()) {
            SecurityUtil.login(hdfsConfig, STORM_KEYTAB_FILE_KEY,
                STORM_USER_NAME_KEY);
        }
        this.fs = FileSystem.get(URI.create(this.fsUrl), hdfsConfig);
    }

    @Override
    public void execute(Tuple tuple) {
        try {
            byte[] bytes = this.format.format(tuple);
            out.write(bytes);
            this.offset += bytes.length;

            this.collector.ack(tuple);

            if(this.syncPolicy.mark(tuple, this.offset)){
                this.out.hsync();
                this.syncPolicy.reset();
            }
            if(this.rotationPolicy.mark(tuple, this.offset)){
                rotateOutputFile();
                this.offset = 0;
                this.rotationPolicy.reset();
            }
        } catch (IOException e) {
            LOG.warn("write/sync failed.", e);
            this.collector.fail(tuple);
        }
    }

    @Override
    void closeOutputFile() throws IOException {
        this.out.hsync();
        this.out.close();
    }

    @Override
    Path createOutputFile() throws IOException {
        Path path = new Path(this.fileNameFormat.getPath(), this.fileNameFormat.getName(this.rotation, System.currentTimeMillis()));
        this.out = this.fs.create(path);
        return path;
    }
}