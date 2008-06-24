CREATE DATABASE ironrhino DEFAULT CHARSET utf8 COLLATE utf8_general_ci;

use ironrhino;

CREATE TABLE  activemq_lock (
  ID bigint(20) NOT NULL,
  TIME bigint(20) DEFAULT NULL,
  BROKER_NAME varchar(250) DEFAULT NULL,
  PRIMARY KEY (ID)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE  activemq_msgs (
  ID int(11) NOT NULL,
  CONTAINER varchar(250) DEFAULT NULL,
  MSGID_PROD varchar(250) DEFAULT NULL,
  MSGID_SEQ int(11) DEFAULT NULL,
  EXPIRATION bigint(20) DEFAULT NULL,
  MSG longblob,
  PRIMARY KEY (ID),
  KEY ACTIVEMQ_MSGS_MIDX (MSGID_PROD,MSGID_SEQ),
  KEY ACTIVEMQ_MSGS_CIDX (CONTAINER),
  KEY ACTIVEMQ_MSGS_EIDX (EXPIRATION)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE activemq_acks(CONTAINER VARCHAR(250) NOT NULL, CLIENT_ID VARCHAR(250) NOT NULL, SUB_NAME VARCHAR(250) NOT NULL, SELECTOR VARCHAR(250), LAST_ACKED_ID INTEGER, PRIMARY KEY ( CONTAINER, CLIENT_ID, SUB_NAME)) ENGINE=MyISAM DEFAULT CHARSET=latin1;
