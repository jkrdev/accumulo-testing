/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.testing.core.randomwalk.sequential;

import java.net.InetAddress;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.MultiTableBatchWriter;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.testing.core.randomwalk.RandWalkEnv;
import org.apache.accumulo.testing.core.randomwalk.Fixture;
import org.apache.accumulo.testing.core.randomwalk.State;

public class SequentialFixture extends Fixture {

  String seqTableName;

  @Override
  public void setUp(State state, RandWalkEnv env) throws Exception {

    Connector conn = env.getAccumuloConnector();

    String hostname = InetAddress.getLocalHost().getHostName().replaceAll("[-.]", "_");

    seqTableName = String.format("sequential_%s_%s_%d", hostname, env.getPid(), System.currentTimeMillis());
    state.set("seqTableName", seqTableName);

    try {
      conn.tableOperations().create(seqTableName);
      log.debug("Created table " + seqTableName + " (id:" + conn.tableOperations().tableIdMap().get(seqTableName) + ")");
    } catch (TableExistsException e) {
      log.warn("Table " + seqTableName + " already exists!");
      throw e;
    }
    conn.tableOperations().setProperty(seqTableName, "table.scan.max.memory", "1K");

    state.set("numWrites", Long.valueOf(0));
    state.set("totalWrites", Long.valueOf(0));
  }

  @Override
  public void tearDown(State state, RandWalkEnv env) throws Exception {
    // We have resources we need to clean up
    if (env.isMultiTableBatchWriterInitialized()) {
      MultiTableBatchWriter mtbw = env.getMultiTableBatchWriter();
      try {
        mtbw.close();
      } catch (MutationsRejectedException e) {
        log.error("Ignoring mutations that weren't flushed", e);
      }

      // Reset the MTBW on the state to null
      env.resetMultiTableBatchWriter();
    }

    log.debug("Dropping tables: " + seqTableName);

    Connector conn = env.getAccumuloConnector();

    conn.tableOperations().delete(seqTableName);
  }
}
