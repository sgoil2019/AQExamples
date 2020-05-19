Rem
Rem $Header: tkmain_8/tkaq/sql/aqjmsdmo.sql /main/3 2020/04/28 18:00:47 nenaveen Exp $
Rem
Rem aqjmsdmo.sql
Rem
Rem Copyright (c) 2000, 2020, Oracle and/or its affiliates. 
Rem All rights reserved.
Rem
Rem    NAME
Rem      aqjmsdmo.sql - <one-line expansion of the name>
Rem
Rem    DESCRIPTION
Rem      <short description of component this file declares/defines>
Rem
Rem    NOTES
Rem      <other useful comments, qualifications, etc.>
Rem
Rem    MODIFIED   (MM/DD/YY)
Rem    nenaveen    04/26/20 - unhide the password.
Rem    ravkotha    09/02/17 - fix for 26024347
Rem    pyeleswa    07/13/16 - move aq demo files from rdbms/demo to
Rem                           test/tkaq/src
Rem    rbhyrava    04/26/11 - tablespace
Rem    aatam       06/05/07 - password need to be consistent
Rem    rbhyrava    11/16/04 - user creation 
Rem    rbhyrava    07/10/00 - AQ JMS demo -setup
Rem    rbhyrava    07/10/00 - Created
Rem
REM =====================================================
REM SETUP for AQ JMS Demos:create user and payload types 
REM =====================================================
SET echo on;
ACCEPT sysusername CHAR PROMPT 'Enter System User:'
ACCEPT syspassword CHAR PROMPT 'Enter System password:' 
ACCEPT username CHAR PROMPT 'Enter AQ user:'
ACCEPT password CHAR PROMPT 'Enter AQ password:'
ACCEPT tns_alias CHAR PROMPT 'Enter TNS Alias:'
CONNECT &sysusername/&syspassword@&tns_alias as sysdba;
SET SERVEROUTPUT ON
SET ECHO ON
SET VERIFY OFF
DROP USER &username CASCADE ;
CREATE USER &username IDENTIFIED BY &password;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE, AQ_ADMINISTRATOR_ROLE TO &username;

CONNECT &username/&password@&tns_alias;

REM ==============================================
REM SETUP complete 
REM ==============================================

EXIT;
