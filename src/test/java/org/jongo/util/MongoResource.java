/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jongo.util;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.NullProcessor;
import de.flapdoodle.embed.process.runtime.Network;

import java.net.UnknownHostException;

public class MongoResource {

    public DB getDb(String dbname) {
        return getInstance().getDB(dbname);
    }

    private Mongo getInstance() {
        String isLocal = System.getProperty("jongo.test.local.mongo");
        if (isLocal != null && isLocal.equals("true")) {
            return LocalMongo.instance;
        } else {
            return EmbeddedMongo.instance;
        }
    }

    /**
     * Launches an embedded Mongod server instance in a separate process.
     *
     * @author Alexandre Dutra
     */
    private static class EmbeddedMongo {

        public static final Version DEFAULT_VERSION = Version.V2_2_4;

        private static Mongo instance = getInstance();

        private static Mongo getInstance() {
            try {

                int port = RandomPortNumberGenerator.pickAvailableRandomEphemeralPortNumber();
                IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                        .defaults(Command.MongoD)
                        .processOutput(new ProcessOutput(new NullProcessor(), new NullProcessor(), new NullProcessor()))//no logs
                        .build();
                MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
                MongodConfig config = new MongodConfig(DEFAULT_VERSION, port, Network.localhostIsIPv6());
                MongodExecutable exe = runtime.prepare(config);
                exe.start();

                return createClient(port);

            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Embedded Mongo instance: " + e, e);
            }
        }
    }

    public static class LocalMongo {

        public static Mongo instance = getInstance();

        private static Mongo getInstance() {
            try {
                return createClient(27017);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize local Mongo instance: " + e, e);
            }
        }
    }

    /**
     * We use deprecated Mongo constructor to ensure backward compatibility with old drivers during compatibility tests.
     * see src/test/sh/run-tests-against-all-driver-versions.sh
     */
    private static Mongo createClient(int port) throws UnknownHostException {
        Mongo mongo = new Mongo("127.0.0.1", port);
        mongo.setWriteConcern(WriteConcern.FSYNC_SAFE);
        return mongo;
    }

}
