/* $Header: tkmain_8/tkaq/src/aqjmsdemo12.java /main/2 2017/10/28 07:02:00 ravkotha Exp $ */

/* Copyright (c) 2012, 2017, Oracle and/or its affiliates. 
All rights reserved.*/

/*
   DESCRIPTION
    Sample demo for 12.1 sharded queue (mulitple consumer, ie. topic)

    This demo does the following:
      -- Setup a multiple consumer sharded queue
      -- Create a topic subscriber and subscribe to the messages published to the topic
      -- Create a topic publisher and publish messages of type
         javax.jms.BytesMessage
         javax.jms.TextMessage
         javax.jms.StreamMessage
         javax.jms.MapMessage
         javax.jms.ObjectMessage

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
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import oracle.jms.AQjmsDestination;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsSession;

import java.util.Properties;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *  @version $Header: tkmain_8/tkaq/src/aqjmsdemo12.java /main/2 2017/10/28 07:02:00 ravkotha Exp $
 *  @author  pabhat
 *  @since   release specific (what release of product did this appear in)
 */
public class aqjmsdemo12 {

    public static void main(String args[]) {
	
	 Console console = System.console();

        console.printf("Enter Jms  User: ");
        String username = console.readLine();
        console.printf("Enter Jms user  password: ");
        char[] passwordChars = console.readPassword();
        String password = new String(passwordChars);

        String topicName = "multiconsumerqueue";
        TopicSession topicSession = null;
        TopicConnectionFactory topicConnectionFactory = null;
        TopicConnection topicConnection = null;

        try {
          String myjdbcURL = System.getProperty("JDBC_URL");
          
          if (myjdbcURL == null )
            System.out.println("The system property JDBC_URL has not been set, Usage:java -DJDBC_URL=xxx filename ");
          else {
            Properties myProperties = new Properties();
            myProperties.put("user", username);
            myProperties.put("password", password);

            topicConnectionFactory = AQjmsFactory.getTopicConnectionFactory(myjdbcURL, myProperties);
            topicConnection = topicConnectionFactory.createTopicConnection(username,password);

            /* Create a Queue Session */
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            topicConnection.start();
            setupMultiConsumerShardedQueue(topicSession, topicName);
            performJmsOperations(topicSession, topicName,username);
          }
        } catch (Exception exception) {
            System.out.println("Exception-1: " + exception);
            exception.printStackTrace();
        } finally {
            try {
                topicSession.close();
                topicConnection.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        System.out.println("\nEnd of Demo aqjmsdemo12.");
    }

    /**
     * Create and start a multiple consumer sharded queue
     *
     * @param topicSession TopicSession
     * @param topicName Topic Name
     * @throws Exception
     */
    public static void setupMultiConsumerShardedQueue(TopicSession topicSession,
            String topicName) throws Exception {
        Topic topic;
        try {
            System.out.println("Creating the Multiple Consumer Sharded Queue " + topicName + "...");
            topic = (Topic) ((AQjmsSession) topicSession).createJMSShardedQueue(topicName, true);

            /* Start the newly created multiple consumer sharded queue */
            System.out.println("\nStarting the Multiple Consumer Sharded Queue " + topicName + "...");
            ((AQjmsDestination) topic).start(topicSession, true, true);

            System.out.println("\nsetupMultiConsumerShardedQueue completed successfully.");
        } catch (Exception exception) {
            System.out.println("Error in setupMultiConsumerShardedQueue: " + exception);
            throw exception;
        }
    }

    /**
     * Perform basic JMS operations on the multiple consumer sharded queue
     *
     * @param topicSession TopicSession
     * @param topicName Topic Name
     */
    public static void performJmsOperations(TopicSession topicSession, String topicName,String user) {
        String subscriber = "demosubcriber";
        try {
            Topic topic = (Topic) ((AQjmsSession) topicSession).getTopic(user, topicName);

            TopicSubscriber topicSubscriber = topicSession.createDurableSubscriber(topic, subscriber);
            System.out.println("\nCreated durable subscriber " + subscriber);
            topicSubscriber.close();

            TopicPublisher publisher = ((TopicSession)topicSession).createPublisher(topic);;

            // BytesMessage
            BytesMessage byteMessage = topicSession.createBytesMessage();
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
            System.out.println("\nSending a BytesMessage ...");
            publisher.publish(byteMessage);

            // TextMessage
            TextMessage textMessage = topicSession.createTextMessage();
            String payLoadForTextMessage = "Sample payload for TextMessage";
            textMessage.setText(payLoadForTextMessage);
            textMessage.setStringProperty("stringproperty", payLoadForTextMessage);
            System.out.println("\nSending a TextMessage ...");
            publisher.publish(textMessage);

            // StreamMessage
            StreamMessage streamMessage = topicSession.createStreamMessage();
            streamMessage.writeBoolean(false);
            Double doubleValue = 123.456789e22;
            streamMessage.writeDouble(doubleValue);

            int intValue = 1234;
            streamMessage.writeInt(intValue);

            char charValue = 'p';
            streamMessage.writeChar(charValue);

            streamMessage.setIntProperty("Make",2012);
            streamMessage.setStringProperty("Name","Ford");
            System.out.println("\nSending a StreamMessage ...");
            publisher.publish(streamMessage);

            // MapMessage
            MapMessage mapMessage = topicSession.createMapMessage();
            mapMessage.setIntProperty("Make",2012);
            mapMessage.setStringProperty("Name","Honda");
            System.out.println("\nSending a MapMessage ...");
            publisher.publish(mapMessage);

            // ObjectMessage
            String objectSample = "String is an object...";
            ObjectMessage objectMessage = topicSession.createObjectMessage();
            objectMessage.setObject(objectSample);
            System.out.println("\nSending an ObjectMessage ...");
            publisher.publish(objectMessage);

            publisher.close();

            // Receive the messages, in the same order in which they are sent
            topicSubscriber = topicSession.createDurableSubscriber(topic, subscriber);

            // BytesMessage
            BytesMessage receivedBytesMessage = (BytesMessage) topicSubscriber.receive();
            StringBuffer buffer = new StringBuffer();

            for (int i = 0; i < (int)receivedBytesMessage.getBodyLength(); i++) {
                buffer.append((char) receivedBytesMessage.readByte());
            }

            String payLoad = buffer.toString().trim();
            System.out.println("\nReceived payload : " + payLoad + " from BytesMessage");
            System.out.println("Received Age : " + receivedBytesMessage.getIntProperty("Age") + " from BytesMessage");
            System.out.println("Received Name : " + receivedBytesMessage.getStringProperty("Name") + " from BytesMessage");
            System.out.println("Received a BytesMessage successfully.");

            // TextMessage
            TextMessage receivedTextMessage = (TextMessage) topicSubscriber.receive();
            payLoad =  receivedTextMessage.getText();

            System.out.println("\nReceived payload : " + payLoad + " from TextMessage");
            System.out.println("Received stringproperty : " + receivedTextMessage.getStringProperty("stringproperty")
                    + " from TextMessage");
            System.out.println("Received a TextMessage successfully.");

            // StreamMessage
            StreamMessage receivedStreamMessage = (StreamMessage) topicSubscriber.receive();
            System.out.println("\nReceived readBoolean : " + receivedStreamMessage.readBoolean() + " from StreamMessage");
            System.out.println("Received readDouble : " + receivedStreamMessage.readDouble() + " from StreamMessage");
            System.out.println("Received readInt : " + receivedStreamMessage.readInt() + " from StreamMessage");
            System.out.println("Received readChar : " + receivedStreamMessage.readChar() + " from StreamMessage");
            System.out.println("Received Make : " + receivedStreamMessage.getIntProperty("Make") + " from StreamMessage");
            System.out.println("Received Name : " + receivedStreamMessage.getStringProperty("Name") + " from StreamMessage");
            System.out.println("Received a StreamMessage successfully.");

            //MapMessage
            MapMessage receivedMapMessage = (MapMessage) topicSubscriber.receive();
            System.out.println("\nReceived Make : " + receivedMapMessage.getIntProperty("Make") + " from MapMessage");
            System.out.println("Received Name : " + receivedMapMessage.getStringProperty("Name") + " from MapMessage");
            System.out.println("Received a MapMessage successfully.");

            // ObjectMessage
            ObjectMessage receivedObjectMessage = (ObjectMessage) topicSubscriber.receive();
            System.out.println("\nReceived object : " + receivedObjectMessage.getObject().equals(objectSample) + " from ObjectMessage");
            System.out.println("Received an ObjectMessage successfully.");

            // Stop the queue
            ((AQjmsDestination) topic).stop(topicSession, true, true, true);
            System.out.println("\nQueue stopped successfully.");

            // Drop the queue created by the demo.
            ((AQjmsDestination) topic).drop(topicSession);
            System.out.println("\nQueue dropped successfully.");
            topicSubscriber.close();
        } catch (Exception e) {
            System.out.println("Error in performJmsOperations: " + e);
        }
    }
}

