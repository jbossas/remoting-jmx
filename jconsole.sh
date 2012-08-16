#!/bin/sh

CLASSPATH=$JAVA_HOME/lib/jconsole.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
CLASSPATH=$CLASSPATH:target/remoting-jmx-1.0.5.CR1-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/logging/jboss-logging/3.1.1.GA/jboss-logging-3.1.1.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/remoting3/jboss-remoting/3.2.8.GA/jboss-remoting-3.2.8.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/xnio/xnio-api/3.0.5.GA/xnio-api-3.0.5.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/xnio/xnio-nio/3.0.5.GA/xnio-nio-3.0.5.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/sasl/jboss-sasl/1.0.1.Final/jboss-sasl-1.0.1.Final.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/marshalling/jboss-marshalling/1.3.15.GA/jboss-marshalling-1.3.15.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/marshalling/jboss-marshalling-river/1.3.15.GA/jboss-marshalling-river-1.3.15.GA.jar


echo CLASSPATH $CLASSPATH

jconsole -J-Djava.class.path=$CLASSPATH

