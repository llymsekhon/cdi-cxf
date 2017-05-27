/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.lilly;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.stereotype.Component;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueueConnectionFactory;

//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import io.swagger.annotations.ApiResponse;
//import io.swagger.annotations.ApiResponses;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponses;
import com.wordnik.swagger.annotations.ApiResponse;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Java class with be hosted in the URI path defined by the @Path annotation. @Path annotations on the methods
 * of this class always refer to a path relative to the path defined at the class level.
 * <p/>
 * For example, with 'http://localhost:8181/cxf' as the default CXF servlet path and '/crm' as the JAX-RS server path,
 * this class will be hosted in 'http://localhost:8181/cxf/crm/helloservice'.  An @Path("/customers") annotation on
 * one of the methods would result in 'http://localhost:8181/cxf/crm/helloservice/hello'.
 */
//@Component
@SuppressWarnings("deprecation")
@Path("/helloservice/")
@Api(value = "/helloservice", description = "Operations about customerservice")
@Consumes({ MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
public class HelloService {
	
    private static final Logger LOG = LoggerFactory.getLogger(HelloService.class);

    /**
     * This method is mapped to an HTTP GET of 'http://localhost:8181/cxf/crm/helloservice/hello/{name}'.  The value for
     * {id} will be passed to this message as a parameter, using the @PathParam annotation.
     * <p/>
     * The method returns a Customer object - for creating the HTTP response, this object is marshaled into XML using JAXB.
     * <p/>
     * For example: surfing to 'http://localhost:8181/cxf/crm/helloservice/hello/OpenShift' will show you the information of
     * Name OpenShift in XML format.
     */
    @GET
    @Path("/hello/{name}/")
    @Produces("application/xml")
    @ApiOperation(value = "Print Customer Name", notes = "More notes about this method")
    @ApiResponses(value = {
      @ApiResponse(code = 500, message = "No Name supplied"),
    })
    public Response getMsg(@ApiParam(value = "OpenShift") @PathParam("name") String name) {
        LOG.info("Invoking getName, Name of Person : {}", name);
        String output = "Hello : " + name;       
        Response r = Response.ok().entity(output).build();
        return r;  
    }
    
    @POST
    @Path("/create")
    public Response wmqCreate() throws JMSException{
    	
    	String password = System.getenv("SECRET_WMQ_KEYSTORE_PWD");
    	String JKSPath = System.getenv("SECRET_WMQ_KEYSTORE_PATH");
    	
		String QMGRNAME = "DMQMC005";
		String HOSTNAME = "fteqadev05.am.lilly.com";
		String CHANNEL = "CH.LLY.IEP.DMQMC005";
		int PORT = 1420;
		String SSLCIPHERSUITE = "SSL_RSA_WITH_3DES_EDE_CBC_SHA";
		String SSLPEERNAME = "CN=*.lilly.com,O=Eli Lilly and Company,L=Indianapolis,ST=Indiana,C=US";

		QueueConnection connection = null;
		QueueSession queueSession = null;
		
		String clrm_queue = "IEP.S02.SYS.ODM.COMMAND";
		String msg="<test>WMQ Standalone test </test>";
		try {
			
			//#### 1. Establish connection to WMQ 
			Class.forName("com.sun.net.ssl.internal.ssl.Provider");

			System.out.println("01. JSSE is installed correctly!");
			char[] KSPW = password.toCharArray();

			// instantiate a KeyStore with type JKS & load the contents of the
			// KeyStore
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(JKSPath), KSPW);
			System.out.println("02. Number of keys on JKS: "+ Integer.toString(ks.size()));

			// Create a keystore object for the truststore
			KeyStore trustStore = KeyStore.getInstance("JKS");
			trustStore.load(new FileInputStream(JKSPath), null);

			// Create a default trust and key manager
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());

			// Initialise the managers
			trustManagerFactory.init(trustStore);
			keyManagerFactory.init(ks, KSPW);

			// Initialise our SSL context from the key/trust managers
			SSLContext sslContext = SSLContext.getInstance("SSLv3");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			// Get an SSLSocketFactory to pass to WMQ
			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			
			// Create default MQ connection factory & Customize the factory
			MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
			factory.setSSLSocketFactory(sslSocketFactory);
			factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
			factory.setQueueManager(QMGRNAME);
			factory.setHostName(HOSTNAME);
			factory.setChannel(CHANNEL);
			factory.setPort(PORT);
			factory.setSSLFipsRequired(false);
			factory.setSSLCipherSuite(SSLCIPHERSUITE);
			factory.setSSLPeerName(SSLPEERNAME);
			factory.setUseConnectionPooling(true);
			System.out.println("04. Factory Object settings completed");
			
			connection = factory.createQueueConnection();
			System.out.println("05. Queue Connection successfully created");

			connection.start();
			System.out.println("06. JMS SSL client connection started!");
			
			//#### 2. Publish messages to WMQ
			queueSession = connection.createQueueSession(true,	QueueSession.CLIENT_ACKNOWLEDGE);
			Queue fqueue = queueSession.createQueue(clrm_queue);
		    MessageProducer finalProducer = queueSession.createProducer(fqueue);
			
		    //Create text message
		    TextMessage textMessage = queueSession.createTextMessage(msg);
		    textMessage.setJMSType("XML");
		    textMessage.setJMSExpiration(2*1000);
		    textMessage.setJMSDeliveryMode(DeliveryMode.PERSISTENT); 
		    finalProducer.send(textMessage);
		            
			//log.info "messsage sent :" + textMessage
		    String jmsCorrelationID = " JMSCorrelationID = '" + textMessage.getJMSMessageID() + "'";
			System.out.println("message sent" +jmsCorrelationID );

			queueSession.commit();
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			queueSession.close();
			connection.close();
		}
		
		Response r = Response.ok().entity("Message successfully sent").build();
    	return r; 
    	
    }
}
