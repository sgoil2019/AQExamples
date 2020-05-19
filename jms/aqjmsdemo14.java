
/* Copyright (c) 2012, 2017, Oracle and/or its affiliates. 
All rights reserved.*/

/*
   DESCRIPTION
    Sample demo for Message Listener with 12.1 sharded queue

    This demo does the following:
      -- Setup a single consumer sharded queue
      -- Create a message producer and send text messages by setting message properties
      -- Create message consumer for the sharded queue using a message selector 
      -- Setup the msaage listener for message consumer and recieve the messages

   MODIFIED    (MM/DD/YY)
 */
import java.lang.*;
import javax.jms.*;
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

public class aqjmsdemo14 {

    public static void main(String args[]) {
        
         Console console = System.console();

        console.printf("Enter Jms  User: ");
        String username = console.readLine();
        console.printf("Enter Jms user  password: ");
        char[] passwordChars = console.readPassword();
        String password = new String(passwordChars);

        String queueName = "msgListernerDemo";
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
            queueSession = queueConnection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);

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
        System.out.println("\nEnd of Demo aqjmsdemo14.");
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
     * Note that, there is only matching selector for the third and forth  message sent to the queue in the example 
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
	    queueSession.commit();

            textMessage = queueSession.createTextMessage();
            textMessage.setJMSType("test");
            textMessage.setBooleanProperty("demoBooleanProperty", true);
            textMessage.setStringProperty("color", "white");
            textMessage.setIntProperty("make", 2012);
            textMessage.setText("This is the second message");
            producer.send(textMessage);
            queueSession.commit();

            textMessage = queueSession.createTextMessage();
            textMessage.setBooleanProperty("demoBooleanProperty", true);
            textMessage.setJMSType("foo");
            textMessage.setStringProperty("color", "red");
            textMessage.setIntProperty("make", 2012);
            textMessage.setText("This is the third message");
            producer.send(textMessage);
	    queueSession.commit();

            textMessage = queueSession.createTextMessage();
            textMessage.setBooleanProperty("demoBooleanProperty", true);
            textMessage.setJMSType("bar");
            textMessage.setStringProperty("color", "blue");
            textMessage.setIntProperty("make", 2011);
            textMessage.setText("This is the fourth message");
            producer.send(textMessage);
            queueSession.commit();

            textMessage = queueSession.createTextMessage();
            textMessage.setBooleanProperty("demoBooleanProperty", false);
            textMessage.setJMSType("_foo%bar");
            textMessage.setStringProperty("color", "silver");
            textMessage.setIntProperty("make", 2012);
            textMessage.setText("This is the fifth message");
            producer.send(textMessage);
	    queueSession.commit();

            System.out.println("\nFinished sending all the 5 messages of type TextMessage ...");
            producer.close();

            TextMessage receivedMessage = null;

            // For the demonstration of Message Selectos, we will create the consumer and receive the messages sequentially.

            // This consumer will get third and fourth message
            String messageSelector = "demoBooleanProperty=TRUE AND JMSType IN ('foo','bar')";
            MessageConsumer firstConsumer = queueSession.createConsumer(queue, messageSelector);
            // Set up MessageListener for MessageConsumer
	    MessageListener mLsnr = new MsgListener(queueSession);
	    firstConsumer.setMessageListener(mLsnr);

      try {
         Thread.sleep(25000) ;
      } catch (InterruptedException e) {} ;

      System.out.println("Successfully dequeued with MessageListener") ;
	    //Close the consumer
	     firstConsumer.close();
            // Stop the queue
            ((AQjmsDestination) queue).stop(queueSession, true, true, true);
            System.out.println("\nQueue stopped successfully.");

            // Drop the queue created by the demo.
            ((AQjmsDestination) queue).drop(queueSession);
            System.out.println("\nQueue dropped successfully.");
            
        } catch (Exception e) {
            System.out.println("Error in performJmsOperations: " + e);
        }
    }
}

class MsgListener implements MessageListener
{

   Session  mysess;
   String              myname;

   MsgListener(Session sess) {
      mysess = sess;
   }
   public void onMessage(javax.jms.Message m)
   {
      TextMessage mm = (TextMessage) m ;
      try {
                    System.out.println("\nReceived demoBooleanProperty : " + mm.getBooleanProperty("demoBooleanProperty"));
                    System.out.println("Received getJMSType : " + mm.getJMSType());
                    System.out.println("Received color : " + mm.getStringProperty("color"));
                    System.out.println("Received make : " + mm.getIntProperty("make"));
                    System.out.println("Received message data : " + mm.getText()); 
         try {
           mysess.commit() ;
         } catch (Exception e) {
           System.out.println("Exception on Message Listener Commit " + e) ;
         }

      } catch (JMSException e) {
        System.out.println("Exception onMessage:" + e) ;
      }
   }
}

