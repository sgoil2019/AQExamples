Rem This file has some demo code for AQ sharded queues basic functionality

SET ECHO ON
SET FEEDBACK 1
SET NUMWIDTH 10
SET LINESIZE 80
SET TRIMSPOOL ON
SET TAB OFF
SET PAGESIZE 100

connect sys/knl_test7 as sysdba;

DROP USER jmsuser CASCADE;
CREATE USER jmsuser IDENTIFIED BY jmsuser;
GRANT CONNECT, RESOURCE, AQ_ADMINISTRATOR_ROLE, AQ_USER_ROLE to jmsuser;
GRANT EXECUTE ON DBMS_AQADM TO jmsuser;
GRANT EXECUTE ON DBMS_AQ TO jmsuser;
GRANT EXECUTE ON DBMS_LOB TO jmsuser;
GRANT EXECUTE ON DBMS_JMS_PLSQL TO jmsuser;

Rem tablespace can be allocated based on the workload.
GRANT UNLIMITED TABLESPACE TO jmsuser;

connect jmsuser/jmsuser

set ECHO ON
set serveroutput on

Rem JMS supports 5 types of messages, TEXT, BYTE, MAP, STREAM, here we use TEXT message
CREATE OR REPLACE TYPE jms1_tbl AS TABLE OF SYS.AQ$_JMS_TEXT_MESSAGE;
/

Rem Create the sharded queue, add a subscriber, start the queue and enqueue/dequeue messages.
CREATE OR REPLACE TYPE jms1_tbl_byte AS TABLE OF SYS.AQ$_JMS_BYTES_MESSAGE;
/
BEGIN
    dbms_aqadm.create_sharded_queue(queue_name => 'jmsuser.jms_text_que',
                                    multiple_consumers => TRUE);
    dbms_aqadm.add_subscriber(queue_name => 'jmsuser.jms_text_que',
                               subscriber =>  sys.aq$_agent('sub', NULL, NULL));
    dbms_aqadm.start_queue(queue_name => 'jmsuser.jms_text_que');
END;
/

Rem enqueue dequeue using non array interface
CREATE OR REPLACE PROCEDURE enq_jms_text_msg( qname in varchar2,
                                              msgs IN NUMBER,
                                              isUsrpClob boolean,
                                              isPlodBlob boolean)
AS
  eopt     dbms_aq.enqueue_options_t;
  mprop    dbms_aq.message_properties_t;
  payload  SYS.AQ$_JMS_TEXT_MESSAGE;
  header   SYS.AQ$_JMS_HEADER;
  agent    SYS.AQ$_AGENT := SYS.AQ$_AGENT('sub1', rpad('1',1000,'1'), 0) ;
  proplist SYS.AQ$_JMS_USERPROPARRAY  := SYS.AQ$_JMS_USERPROPARRAY() ;
  prop     SYS.AQ$_JMS_USERPROPERTY ;
  b1       raw(10);
  msg_id   RAW(16);
  prpStr   varchar2(20);
  plodlt2k varchar2(512);
  plodgt2k varchar2(32767);
  retval   PLS_INTEGER;
BEGIN

  -- The following code constructs a payload for the message.
  if (isPlodBlob = TRUE) then
    FOR p IN 1..500 LOOP
       plodgt2k := CONCAT (plodgt2k, 'ABCDEFGHIJ');
    END LOOP;
  else
    FOR p IN 1..20 LOOP
       plodlt2k := CONCAT (plodlt2k, '1234567890');
    END LOOP;
  end if;

  if (isUsrpClob = FALSE and isPlodBlob = FALSE) then
    proplist.extend(2);
    prop   := SYS.AQ$_JMS_USERPROPERTY('year', 200, null, 2010, 23) ;
    proplist(1) := prop;
    prop   := SYS.AQ$_JMS_USERPROPERTY('color', 100, 'red', null, 27);
    proplist(2) := prop;
    header := SYS.AQ$_JMS_HEADER(agent, 'NUMBER', 'USR1', 'APP1',
                                 'GROUP1', 2000, proplist) ;
    b1 :=  'ABC' || to_char(1);
    payload := SYS.AQ$_JMS_TEXT_MESSAGE(header, length(b1), b1, null) ;
  else
    payload := sys.aq$_jms_text_message.construct;

    payload.set_replyto(agent);
    payload.set_type('tkaqpet2');
    payload.set_userid('jmsuser');
    payload.set_appid('plsql_enq');
    payload.set_groupid('st');
    payload.set_groupseq(1);

    payload.set_boolean_property('import', True);
    payload.set_string_property('color', 'RED');
    if (isUsrpClob = TRUE) then
      for k in 1..25 loop
         prpStr := 'UsrPrp' || to_char(k);
         payload.set_string_property(prpStr, rpad('A', 200, 'A'));
      end loop;
    end if;
    payload.set_short_property('year', 1999);
    payload.set_long_property('mileage', 300000);
    payload.set_double_property('price', 16999.99);
    payload.set_byte_property('password', 127);

    if (isPlodBlob = TRUE) then
      payload.set_text(plodgt2k); 
    else 
      payload.set_text(plodlt2k);
    end if;
  end if;

  retval := 0;

  for i in 1..msgs loop
    -- Actual enqueue call, this publishes a message in the queue.
    dbms_aq.enqueue(queue_name => qname,
                    enqueue_options    => eopt,
                    message_properties => mprop,
                    payload            => payload,
                    msgid              => msg_id);
    dbms_output.put_line ( 'ENQUEUED ' || to_char(msg_id));
    retval := retval + 1;
    commit;
  end loop;
  dbms_output.put_line ('Number of messages enqueued: ' || retval);
END;
/

show errors;

CREATE OR REPLACE PROCEDURE deq_jms_text_msg( qname in varchar2,
                                              msgs IN NUMBER,
                                              bPrintOut boolean) AS
    id                 pls_integer;
    message            sys.aq$_jms_text_message;
    agent              sys.aq$_agent;
    dequeue_options    dbms_aq.dequeue_options_t;
    message_properties dbms_aq.message_properties_t;
    msgid              raw(16);
    outtext            varchar2(32767);
    retval             PLS_INTEGER;
BEGIN
    DBMS_OUTPUT.ENABLE (20000);

    message := sys.aq$_jms_text_message.construct;
    dequeue_options.consumer_name := 'sub' ;
    dequeue_options.navigation := DBMS_AQ.FIRST_MESSAGE;

    retval := 0;
    for i in 1..msgs loop
      dbms_aq.dequeue(queue_name => qname,
                      dequeue_options => dequeue_options,
                      message_properties => message_properties,
                      payload => message,
                      msgid => msgid);
      retval := retval + 1;

      message.get_text(outtext);
      if (bPrintOut = TRUE) then
        dbms_output.put_line ( 'DEQUEUED ' || to_char(msgid));
        dbms_output.put_line('price: ' || message.get_double_property('price'));
      end if;
    end loop;
    dbms_output.put_line ('Number of messages dequeued: ' || retval);
    COMMIT;
END;
/

COMMIT;

show errors;

Rem enqueue dequeue using non array interface
Rem Usrp <4k, Payload <2k
exec enq_jms_text_msg('jms_text_que', 2, FALSE, FALSE);

Rem Usrp <4k, Payload >2k
exec enq_jms_text_msg('jms_text_que', 2, FALSE, TRUE);

Rem Usrp >4k, Payload <2k
exec enq_jms_text_msg('jms_text_que', 2, TRUE, FALSE);

Rem Usrp >4k, Payload >2k
exec enq_jms_text_msg('jms_text_que', 2, TRUE, TRUE);

exec deq_jms_text_msg('jms_text_que', 8, TRUE);

commit;

/* Stop the Queue */
exec dbms_aqadm.stop_queue('jms_text_que');

/* Drop Queue */
exec dbms_aqadm.drop_sharded_queue('jms_text_que');

connect sys/knl_test7 as sysdba
set echo on
set serveroutput on

drop user jmsuser cascade;

