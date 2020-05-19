/* $Header: tkmain_8/tkaq/src/aqjmsdemo13.java /main/2 2017/10/28 07:02:00 ravkotha Exp $ */

/* Copyright (c) 2012, 2017, Oracle and/or its affiliates. 
All rights reserved.*/

/*
   DESCRIPTION
    Sample demo for Message Selectors with 12.1 sharded queue

    This demo does the following:
      -- Setup a single consumer sharded queue
      -- Create a message producer and send text messages by setting message properties
      -- Create message consumers for the sharded queue using a message selector and receive messages

   NOTES
    The instructions for setting up and running this demo is available in aqjmsREADME.txt.

   MODIFIED    (MM/DD/YY)
    ravkotha    09/05/17 - fix for 26024347
    pyeleswa    07/13/16 - move aq demo files from rdbms/demo to test/tkaq/src
    mpkrishn    01/08/13 - Changes to run in CDB mode, getting JDBC_URL from env
    pabhat      10/05/12 - Demo for Message Selectors
    pabhat      10/05/12 - Creation
 */
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
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
 *  @version $Header: tkmain_8/tkaq/src/aqjmsdemo13.java /main/2 2017/10/28 07:02:00 ravkotha Exp $
 *  @author  pabhat
 *  @since   release specific (what release of product did this appear in)
 */

public class aqjmsdemo13 {

    public static void main(String args[]) {
        
         Console console = System.console();

        console.printf("Enter Jms  User: ");
        String username = console.readLine();
        console.printf("Enter Jms user  password: ");
        char[] passwordChars = console.readPassword();
        String password = new String(passwordChars);

        String queueName = "selectordemo";
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
        System.out.println("\nEnd of Demo aqjmsdemo13.");
    }

    /**
     * Create and start a sharded queue
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
     * Perform basic JMS operations on the single consumer sharded queue, by creating message consumers
     * using message selectors.
     * 
     * Note that, there is no matching selector for the first message sent to the queue in the example 
     * below and is not consumed by any of the consumers.
     *
     * @param queueSession Queue Session
     * @param queueName Queue Name
     */
    public static void performJmsOperations(QueueSession queueSession, String queueName,String user) {

        try {
            Queue queue = (Queue) ((AQjmsSession) queueSession).getQueue(user, queueName);
            MessageProducer producer = queueSession.createProducer(queue);

            System.out.println("\nSending 5 messages of type TextMessage ...");

            TextMessage textMessage = queueSession.createTextMessage();
            textMessage = queueSession.createTextMessage();
            textMessage.setText("This is the first message");
            producer.send(textMessage);

            textMessage = queueSession.createTextMessage();
            textMessage.setJMSType("test");
            textMessage.setBooleanProperty("demoBooleanProperty", true);
            textMessage.setStringProperty("color", "white");
            textMessage.setIntProperty("make", 2012);
            textMessage.setText("This is the second message");
            producer.send(textMessage);

            textMessage = queueSession.createTextMessage();
            textMessage.setBooleanProperty("demoBooleanProperty", true);
            textMessage.setJMSType("foo");
            textMessage.setStringProperty("color", "red");
            textMessage.setIntProperty("make", 2012);
            textMessage.setText("This is the third message");
            producer.send(textMessage);

            textMessage = queueSession.createTextMessage();
            textMessage.setBooleanProperty("demoBooleanProperty", true);
            textMessage.setJMSType("bar");
            textMessage.setStringProperty("color", "blue");
            textMessage.setIntProperty("make", 2011);
            textMessage.setText("This is the fourth message");
            producer.send(textMessage);

            textMessage = queueSession.createTextMessage();
            textMessage.setBooleanProperty("demoBooleanProperty", false);
            textMessage.setJMSType("_foo%bar");
            textMessage.setStringProperty("color", "silver");
            textMessage.setIntProperty("make", 2012);
            textMessage.setText("This is the fifth message");
            producer.send(textMessage);

            System.out.println("\nFinished sending all the 5 messages of type TextMessage ...");
            producer.close();

            TextMessage receivedMessage = null;

            // For the demonstration of Message Selectos, we will create the consumer and receive the messages sequentially.

            // This consumer will get third and fourth message
            String messageSelector = "demoBooleanProperty=TRUE AND JMSType IN ('foo','bar')";
            MessageConsumer firstConsumer = queueSession.createConsumer(queue, messageSelector);

            while (true) {
                receivedMessage = (TextMessage) firstConsumer.receive(3000);
                if (receivedMessage == null) {
                    break;
                } else {
                    System.out.println("\nReceived demoBooleanProperty : " + receivedMessage.getBooleanProperty("demoBooleanProperty"));
                    System.out.println("Received getJMSType : " + receivedMessage.getJMSType());
                    System.out.println("Received color : " + receivedMessage.getStringProperty("color"));
                    System.out.println("Received make : " + receivedMessage.getIntProperty("make"));
                    System.out.println("Received message data : " + receivedMessage.getText());
                }
            }

            // This consumer will get only fifth message
            messageSelector = "JMSType LIKE 'A_fo_A%b%' ESCAPE 'A'";
            MessageConsumer secondConsumer = queueSession.createConsumer(queue, messageSelector);

            while (true) {
                receivedMessage = (TextMessage) secondConsumer.receive(3000);
                if (receivedMessage == null) {
                    break;
                } else {
                    System.out.println("\nReceived demoBooleanProperty : " + receivedMessage.getBooleanProperty("demoBooleanProperty"));
                    System.out.println("Received getJMSType : " + receivedMessage.getJMSType());
                    System.out.println("Received color : " + receivedMessage.getStringProperty("color"));
                    System.out.println("Received make : " + receivedMessage.getIntProperty("make"));
                    System.out.println("Received message data : " + receivedMessage.getText());
                }
            }

            // This consumer will get only second message
            messageSelector = "JMSType = 'test' AND color = 'white' AND make >= 1200";
            MessageConsumer thirdConsumer = queueSession.createConsumer(queue, messageSelector);
            while (true) {
                receivedMessage = (TextMessage) thirdConsumer.receive(3000);
                if (receivedMessage == null) {
                    break;
                } else {
                    System.out.println("\nReceived demoBooleanProperty " + receivedMessage.getBooleanProperty("demoBooleanProperty"));
                    System.out.println("Received getJMSType " + receivedMessage.getJMSType());
                    System.out.println("Received color " + receivedMessage.getStringProperty("color"));
                    System.out.println("Received make " + receivedMessage.getIntProperty("make"));
                    System.out.println("Received message data : " + receivedMessage.getText());
                }
            }

            // Stop the queue
            ((AQjmsDestination) queue).stop(queueSession, true, true, true);
            System.out.println("\nQueue stopped successfully.");

            // Drop the queue created by the demo.
            ((AQjmsDestination) queue).drop(queueSession);
            System.out.println("\nQueue dropped successfully.");
            
            // Close the consumers.
            firstConsumer.close();
            secondConsumer.close();
            thirdConsumer.close();
        } catch (Exception e) {
            System.out.println("Error in performJmsOperations: " + e);
        }
    }
}
