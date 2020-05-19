/* $Header: tkmain_8/tkaq/src/aqjmsdemo11.java /main/2 2017/10/28 07:02:00 ravkotha Exp $ */

/* Copyright (c) 2012, 2017, Oracle and/or its affiliates. 
All rights reserved.*/

/*
   DESCRIPTION
    Sample demo for 12.1 sharded queue (single consumer)

    This demo does the following:
      -- Setup a single consumer sharded queue
      -- Create a message producer and send messages of type
         javax.jms.BytesMessage
         javax.jms.TextMessage
         javax.jms.StreamMessage
         javax.jms.MapMessage
         javax.jms.ObjectMessage
       -- Create a message consumer and receive the messages sent by the producer

   NOTES
    The instructions for setting up and running this demo is available in aqjmsREADME.txt.

   MODIFIED    (MM/DD/YY)
    ravkotha    09/05/17 - fix for 26024347
    pyeleswa    04/11/16 - move aq demo files from rdbms/demo to test/tkaq/src
    mpkrishn    01/08/13 - Changes to run in CDB mode, getting JDBC_URL from env
    pabhat      09/27/12 - Creation

 */
import java.io.UnsupportedEncodingException;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import oracle.jms.AQjmsDestination;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;

import java.util.Properties;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *  @version $Header: tkmain_8/tkaq/src/aqjmsdemo11.java /main/2 2017/10/28 07:02:00 ravkotha Exp $
 *  @author  pabhat
 *  @since   release specific (what release of product did this appear in)
 */
public class aqjmsdemo11 {

    public static void main(String args[]) {
	
	Console console = System.console();

        console.printf("Enter Jms  User: ");
        String username = console.readLine();
        console.printf("Enter Jms user  password: ");
        char[] passwordChars = console.readPassword();
        String password = new String(passwordChars);

        String queueName = "shardedqueue";
        QueueSession queueSession = null;
        QueueConnectionFactory queueConnectionFactory = null;
        QueueConnection queueConnection = null;
        try {
           String myjdbcURL = System.getProperty("JDBC_URL");
		   
           if (myjdbcURL == null )
             System.out.println("The system property JDBC_URL has not been set, Usage:java -DJDBC_URL=xxx filename ");
           else {
              Properties myProperties = new Properties();
              myProperties.put("user", username);
              myProperties.put("password", password);

              queueConnectionFactory = AQjmsFactory.getQueueConnectionFactory(myjdbcURL, myProperties);
              queueConnection = queueConnectionFactory.createQueueConnection(username,password);

              /* Create a Queue Session */
              queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

              queueConnection.start();
              setupShardedQueue(queueSession, queueName);
              performJmsOperations(queueSession, queueName,username);
           }
        } catch (Exception exception) {
            System.out.println("Exception-1: " + exception);
            exception.printStackTrace();
        } finally {
            try {
                queueSession.close();
                queueConnection.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        System.out.println("\nEnd of Demo aqjmsdemo11.");
    }

    /**
     * Create and start a single consumer sharded queue
     *
     * @param queueSession QueueSession
     * @param queueName Queue Name
     * @throws Exception
     */
    public static void setupShardedQueue(QueueSession queueSession,
            String queueName) throws Exception {
        Queue queue;
        try {
            System.out.println("Creating the Sharded Queue " + queueName + "...");
            queue = (Queue) ((AQjmsSession) queueSession).createJMSShardedQueue(queueName, false);

            /* Start the newly created queue */
            System.out.println("\nStarting the Sharded Queue " + queueName + "...");
            ((AQjmsDestination) queue).start(queueSession, true, true);

            System.out.println("\nsetupShardedQueue completed successfully.");
        } catch (Exception exception) {
            System.out.println("Error in setupShardedQueue: " + exception);
            throw exception;
        }
    }

    /**
     * Perform basic JMS operations on the single consumer sharded queue
     *
     * @param queueSession Queue Session
     * @param queueName Queue Name
     */
    public static void performJmsOperations(QueueSession queueSession, String queueName,String user) {

        try {
            Queue queue = (Queue) ((AQjmsSession) queueSession).getQueue(user, queueName);
            MessageProducer producer = queueSession.createProducer(queue);

            // BytesMessage
            BytesMessage byteMessage = queueSession.createBytesMessage();
            String payloadStr = "Sample payload for BytesMessage";
            byte[] payload = null;
            try {
                payload = payloadStr.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }

            byteMessage.writeBytes(payload);
            byteMessage.setIntProperty("Age", 25);
            byteMessage.setStringProperty("Name", "scott");
            byteMessage.setBooleanProperty("booleanproperty", true);
            System.out.println("\nSending a BytesMessage ...");
            producer.send(byteMessage);

            // TextMessage
            TextMessage textMessage = queueSession.createTextMessage();
            String payLoadForTextMessage = "Sample payload for TextMessage";
            textMessage.setText(payLoadForTextMessage);
            textMessage.setStringProperty("stringproperty", payLoadForTextMessage);
            System.out.println("\nSending a TextMessage ...");
            producer.send(textMessage);

            // StreamMessage
            StreamMessage streamMessage = queueSession.createStreamMessage();
            streamMessage.writeBoolean(false);
            Double doubleValue = 123.456789e22;
            streamMessage.writeDouble(doubleValue);

            int intValue = 1234;
            streamMessage.writeInt(intValue);

            char charValue = 'p';
            streamMessage.writeChar(charValue);

            streamMessage.setIntProperty("Make", 2012);
            streamMessage.setStringProperty("Name", "Ford");
            System.out.println("\nSending a StreamMessage ...");
            producer.send(streamMessage);

            // MapMessage
            MapMessage mapMessage = queueSession.createMapMessage();
            mapMessage.setIntProperty("Make", 2012);
            mapMessage.setStringProperty("Name", "Honda");
            System.out.println("\nSending a MapMessage ...");
            producer.send(mapMessage);

            // ObjectMessage
            String objectSample = "String is an object...";
            ObjectMessage objectMessage = queueSession.createObjectMessage();
            objectMessage.setObject(objectSample);
            System.out.println("\nSending an ObjectMessage ...");
            producer.send(objectMessage);

            producer.close();

            // Receive the messages, in the same order in which they are sent
            MessageConsumer consumer = queueSession.createConsumer(queue);

            // BytesMessage
            BytesMessage receivedBytesMessage = (BytesMessage) consumer.receive();
            StringBuffer buffer = new StringBuffer();

            for (int i = 0; i < (int) receivedBytesMessage.getBodyLength(); i++) {
                buffer.append((char) receivedBytesMessage.readByte());
            }

            String payLoad = buffer.toString().trim();
            System.out.println("\nReceived payload : " + payLoad + " from BytesMessage");
            System.out.println("Received Age : " + receivedBytesMessage.getIntProperty("Age") + " from BytesMessage");
            System.out.println("Received Name : " + receivedBytesMessage.getStringProperty("Name") + " from BytesMessage");
            System.out.println("Received a BytesMessage successfully.");

            // TextMessage
            TextMessage receivedTextMessage = (TextMessage) consumer.receive();
            payLoad = receivedTextMessage.getText();

            System.out.println("\nReceived payload : " + payLoad + " from TextMessage");
            System.out.println("Received stringproperty : " + receivedTextMessage.getStringProperty("stringproperty")
                    + " from TextMessage");
            System.out.println("Received a TextMessage successfully.");

            // StreamMessage
            StreamMessage receivedStreamMessage = (StreamMessage) consumer.receive();
            System.out.println("\nReceived readBoolean : " + receivedStreamMessage.readBoolean() + " from StreamMessage");
            System.out.println("Received readDouble : " + receivedStreamMessage.readDouble() + " from StreamMessage");
            System.out.println("Received readInt : " + receivedStreamMessage.readInt() + " from StreamMessage");
            System.out.println("Received readChar : " + receivedStreamMessage.readChar() + " from StreamMessage");
            System.out.println("Received Make : " + receivedStreamMessage.getIntProperty("Make") + " from StreamMessage");
            System.out.println("Received Name : " + receivedStreamMessage.getStringProperty("Name") + " from StreamMessage");
            System.out.println("Received a StreamMessage successfully.");

            // MapMessage
            MapMessage receivedMapMessage = (MapMessage) consumer.receive();
            System.out.println("\nReceived Make : " + receivedMapMessage.getIntProperty("Make") + " from MapMessage");
            System.out.println("Received Name : " + receivedMapMessage.getStringProperty("Name") + " from MapMessage");
            System.out.println("Received a MapMessage successfully.");

            // ObjectMessage
            ObjectMessage receivedObjectMessage = (ObjectMessage) consumer.receive();
            System.out.println("\nReceived object : " + receivedObjectMessage.getObject().equals(objectSample) + " from ObjectMessage");
            System.out.println("Received an ObjectMessage successfully.");

            // Stop the queue
            ((AQjmsDestination) queue).stop(queueSession, true, true, true);
            System.out.println("\nQueue stopped successfully.");

            // Drop the queue created by the demo.
            ((AQjmsDestination) queue).drop(queueSession);
            System.out.println("\nQueue dropped successfully.");
            consumer.close();
        } catch (Exception e) {
            System.out.println("Error in performJmsOperations: " + e);
        }
    }
}

