package com.avast.client.rabbitmq;

import com.avast.client.api.exceptions.RequestConnectException;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.ExceptionHandler;
import com.rabbitmq.client.Recoverable;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;

import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created <b>15.10.2014</b><br>
 *
 * @author Jenda Kolena, kolena@avast.com
 */
@SuppressWarnings("unused")
@ThreadSafe
public class DefaultRabbitMQSender extends RabbitMQClientBase implements RabbitMQSender {
    protected final Meter sentMeter;
    protected final Meter failedMeter;

    public DefaultRabbitMQSender(final Address[] addresses, final String virtualHost, final String username, final String password, final String queue, final int connectionTimeout, final int recoveryTimeout, final SSLContext sslContext, final ExceptionHandler exceptionHandler, final String jmxGroup) throws RequestConnectException {
        super("SENDER", addresses, virtualHost, username, password, queue, connectionTimeout, recoveryTimeout, sslContext, exceptionHandler, jmxGroup);

        sentMeter = Metrics.newMeter(getMetricName("sent"), "sentMessages", TimeUnit.SECONDS);
        failedMeter = Metrics.newMeter(getMetricName("failed"), "failedMessages", TimeUnit.SECONDS);
    }

    @Override
    public synchronized void send(final String exchange, final byte[] msg, final AMQP.BasicProperties properties) throws IOException {
        LOG.debug("Sending message with length " + (msg != null ? msg.length : 0) + " to " + connection.getAddress().getHostName() + "/" + queue);
        try {
            channel.basicPublish(exchange, queue, properties, msg);
            sentMeter.mark();
        } catch (IOException e) {
            failedMeter.mark();
            LOG.debug("Error while sending the message", e);
            throw e;
        }
    }

    @Override
    public void send(final byte[] msg, final AMQP.BasicProperties properties) throws IOException {
        send("", msg, properties);
    }

    @Override
    public void send(final byte[] msg) throws IOException {
        send(msg, createProperties());
    }

    @Override
    public void send(final String exchange, final byte[] msg) throws IOException {
        send(exchange, msg, createProperties());
    }

    @Override
    public void send(final ByteString msg, final AMQP.BasicProperties properties) throws IOException {
        send(msg.toByteArray(), properties);
    }

    @Override
    public void send(final ByteString msg) throws IOException {
        send(msg.toByteArray(), createProperties());
    }

    @Override
    public void send(final MessageLite msg, final AMQP.BasicProperties properties) throws IOException {
        send(msg.toByteArray(), properties);
    }

    @Override
    public void send(final MessageLite msg) throws IOException {
        send(msg.toByteArray(), createProperties());
    }

    @Override
    protected void onChannelRecovered(Recoverable recoverable) {
        //no extra action
    }

    public static AMQP.BasicProperties createProperties(String msgType, String contentType, String expiration) {
        return new AMQP.BasicProperties.Builder()
                .expiration(expiration)
                .contentType(contentType)
                .contentEncoding("utf-8")
                .deliveryMode(2)
                .correlationId(null)
                .priority(0)
                .type(msgType)
                .timestamp(new Date())
                .build();
    }

    public static AMQP.BasicProperties createProperties(String msgType, String contentType) {
        return createProperties(msgType, contentType, null);
    }

    public static AMQP.BasicProperties createProperties(String msgType) {
        return createProperties(msgType, "application/octet-stream", null);
    }

    public static AMQP.BasicProperties createProperties() {
        return createProperties(null, "application/octet-stream", null);
    }
}
