/
/ $Header: aqjmsShardREADME.txt 18-may-2020.23:03:26 skarnewa Exp $
/
/ aqjmsShardREADME.txt
/
/ Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
/
/   NAME
/     aqjmsShardREADME.txt - README for sharded queue demos
/
/   DESCRIPTION
/     This text explains how to run the demos for sharded queues.
/
/   NOTES
/     <other useful comments, qualifications, etc.>
/
/

The following files are required for running the demos

 aqjmsdmo.sql     - Setup file for AQ JMS demos
 aqjmsdemo11.java - Single consumer sharded queue demo
 aqjmsdemo12.java - Multiple consumer sharded queue demo
 aqjmsdemo13.java - Message Selectors with sharded queue
 aqjmsdemo14.java - MessageListener demo for sharded queue

How to Run the JMS API demos:
-----------------------------
1. Run aqjmsdemo.sql to set up the user

    % sqlplus system/manager @aqjmsdemo.sql
        creates jmsuser user

--Provide system username, password and AQ username and password which needs to be created when prompted while running the aqjmsdemo.sql.

2. To compile the JMS API demos

    % javac -classpath ojdbc8.jar:jta.jar:jmscommon.jar:aqapi.jar aqjmsdemo*.java 

3. To run JMS API demos

   %  java -classpath ojdbc8.jar:jta.jar:jmscommon.jar:aqapi.jar aqjmsdemo* [SID] [HOST] [PORT] [DRIVER]

   <DRIVER> = [oci8|thin]

   Example:
    java -classpath ojdbc8.jar:jta.jar:jmscommon.jar:aqapi.jar aqjmsdemo11 orcl dlsun673 1521 thin

--Provide usename, password of user created while setup, when prompted while running of JMS API demos.
