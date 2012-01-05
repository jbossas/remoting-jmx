#!/bin/sh

CLASSPATH=$JAVA_HOME/lib/jconsole.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
CLASSPATH=$CLASSPATH:target/remoting-jmx-1.0.0.Alpha1-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/logging/jboss-logging/3.1.0.CR2/jboss-logging-3.1.0.CR2.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/remoting3/jboss-remoting/3.2.0.CR8/jboss-remoting-3.2.0.CR8.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/xnio/xnio-api/3.0.0.GA/xnio-api-3.0.0.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/xnio/xnio-nio/3.0.0.GA/xnio-nio-3.0.0.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/sasl/jboss-sasl/1.0.0.Beta9/jboss-sasl-1.0.0.Beta9.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/marshalling/jboss-marshalling/1.3.0.GA/jboss-marshalling-1.3.0.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/jboss/marshalling/jboss-marshalling-river/1.3.0.GA/jboss-marshalling-river-1.3.0.GA.jar


echo CLASSPATH $CLASSPATH

jconsole -J-Djava.class.path=$CLASSPATH

