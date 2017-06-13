/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.rest.client.openstack.vmidc.notification;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public abstract class RabbitMQClient {
    private static final int CLOSE_CONNECTION_TIMEOUT = 1000 * 2; // 2sec timeout

    private static final Logger log = Logger.getLogger(RabbitMQClient.class);

    private static final int RECONNECT_DELAY = 60 * 1000;

    private static final String NEUTRON_EXCHANGE = "neutron";
    private static final String KEYSTONE_EXCHANGE = "keystone";

    private Connection connection = null;
    private Channel channel;
    private String consumerTag;

    private String serverIP;
    private int port;
    private String user;
    private String password;
    private String exchange;
    private String queue;
    private String routingKey;

    protected static final String NOVA_EXCHANGE = "nova";
    protected static final String QUEUE_NAME = "osc_queue";
    protected static final String ROUTING_KEY = "notifications.info";

    /**
     *
     * Generic Rabbit MQ client which will open a connections and Subscribe to the provided Queue with given Topic and
     * routing Key
     *
     * @param serverIP
     *            RabbitMQ server IP
     * @param port
     *            RabbitMQ Server Port
     * @param user
     *            RabbitMQ user name
     * @param password
     *            RabbitMQ password for authentication
     * @param exchange
     *            Exchange name for example "nova". You can find out this by command " sudo rabbitmqctl list_exchanges"
     * @param queue
     *            Name if the Queue to subscribe
     *
     * @param routingKey
     *            Routing key which will be used to route messages by the server
     */
    protected void init(String serverIP, int port, String user, String password, String exchange, String queue,
            String routingKey) {
        this.serverIP = serverIP;
        this.port = port;
        this.user = user;
        this.password = password;
        this.exchange = exchange;
        this.queue = queue;
        this.routingKey = routingKey;
    }

    /**
     *
     * This method will create a Connection with a channel to the provided server and Will provide a consumer to consume
     * Notifications sent on the subscribed Queue
     *
     * @throws IOException
     *
     */
    public void connect() throws IOException {
        ConnectionFactory factory = initConnectionFactory();
        while (true) {
            try {
                if (this.connection == null || !this.connection.isOpen()) {
                    this.connection = factory.newConnection();
                }
                if (this.channel == null || !this.channel.isOpen()) {
                    this.channel = this.connection.createChannel();
                }
                initChannel(this.channel);

                final DefaultConsumer consumer = new DefaultConsumer(this.channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
                            byte[] body) throws IOException {
                        super.handleDelivery(consumerTag, envelope, properties, body);
                        receiveMessage(new String(body));
                    }

                };
                this.consumerTag = this.channel.basicConsume(this.queue, true, consumer);
                log.info("Successfully connected to RabbitMQ Server :- " + this.serverIP);
                return;
            } catch (Exception e) {
                // Ignore connection timeout and retry until you connect...
                log.error("Failed to connect to Rabbit MQ server '" + this.serverIP + "' Error:" + e.getMessage());
                generateConnectionFailureAlert();
                try {
                    Thread.sleep(RECONNECT_DELAY);
                } catch (InterruptedException ex) {
                    log.error("RabbitMQ connect was interrupted! '" + this.serverIP + "' Error:" + e.getMessage());
                    return;
                }
            }
        }
    }

    /**
     * Tests connection and closes the connection immediately after.
     *
     * @throws Exception
     *             incase connection is not successful.
     */
    public void testConnection() throws Throwable {
        ConnectionFactory factory = initConnectionFactory();
        factory.setAutomaticRecoveryEnabled(false);
        Connection testConnection = null;
        Channel testChannel = null;

        try {
            testConnection = factory.newConnection();
            testChannel = testConnection.createChannel();
            initChannel(testChannel);
            log.info("Successfully connected to RabbitMQ Server :- " + this.serverIP);
        } catch (IOException ioe) {
            log.error("Failed to connect to Rabbit MQ server '" + this.serverIP + "' IO Error." + ioe.getMessage());
            throw ioe.getCause() == null ? ioe : ioe.getCause();
        } catch (Exception e) {
            log.error("Failed to connect to Rabbit MQ server '" + this.serverIP + "' Error." + e.getMessage());
            throw e;
        } finally {
            close(testConnection, testChannel);
        }
    }

    /**
     * Close Channel and then Close Connection
     */
    public void close() {
        close(this.connection, this.channel);
    }

    public String getServerIP() {
        return this.serverIP;
    }

    public String getPassword() {
        return this.password;
    }

    public int getPort() {
        return this.port;
    }

    public String getUser() {
        return this.user;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    private ConnectionFactory initConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        // TODO: Future. use SSL if vc is HTTPS
        factory.setHost(this.serverIP);
        factory.setPort(this.port);
        factory.setUsername(this.user);
        factory.setPassword(this.password);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setThreadFactory(
                new ThreadFactoryBuilder().setNameFormat("RabbitMQ-Thread-Pool " + this.serverIP + " - %d").build());
        return factory;
    }

    private void initChannel(Channel channel) throws IOException {
        channel.basicQos(1);
        // this.channel.exchangeDeclare(this.exchange, TOPIC);
        channel.queueDeclare(QUEUE_NAME, true, true, true, null);
        channel.queueBind(QUEUE_NAME, NOVA_EXCHANGE, ROUTING_KEY);
        channel.queueBind(QUEUE_NAME, NEUTRON_EXCHANGE, ROUTING_KEY);
        channel.queueBind(QUEUE_NAME, KEYSTONE_EXCHANGE, ROUTING_KEY);
        channel.queueBind(this.queue, this.exchange, this.routingKey);
    }

    private void close(Connection connection, Channel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                if (this.consumerTag != null) {
                    channel.basicCancel(this.consumerTag);
                    this.consumerTag = null;
                }
                log.info("Closing RabbitMQ Channel - " + this.serverIP);
                channel.close();
                this.channel = null;
            }
            if (connection != null && connection.isOpen()) {
                log.info("Closing RabbitMQ Connection - " + this.serverIP);
                connection.close(CLOSE_CONNECTION_TIMEOUT);
                this.connection = null;
            }
        } catch (Exception e) {
            log.error("Failed to close RabbitMQ connections " + this.serverIP, e);
        }
    }

    protected abstract void receiveMessage(String message);

    protected abstract void generateConnectionFailureAlert();

}
