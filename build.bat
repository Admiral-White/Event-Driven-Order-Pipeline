@echo off
set MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT
mvn clean install
