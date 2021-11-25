/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messagingservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 *
 * @author Vorno
 */
public class ReceiveFromQueue {
    private static BufferedReader in;
    private static PrintWriter out;
    private static ConnectionFactory connectionFactory;
    private static Queue queue;
    //constructor 
    public ReceiveFromQueue(BufferedReader in, PrintWriter out, ConnectionFactory connectionFactory, Queue queue) {
        this.in = in;
        this.out = out;
        this.connectionFactory = connectionFactory;
        this.queue = queue;
        receiveFromQueue();
    }
    //allows client to receive messages - establishes connection to connection factory and receives messages
    public synchronized void receiveFromQueue() { //receiver
        Connection conn = null;
        Session session = null;
        MessageConsumer consumer = null;
        
        try {
            conn = connectionFactory.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(queue);
            consumer.setMessageListener(new IncomingListener());
            conn.start();
            
            try {
                out.println("//success");
                out.println("//success");
                Thread.sleep(15000);
                out.println("//complete");
            } catch(InterruptedException e) {
                
            }
        } catch(JMSException e) {
            System.err.println("Unable to open Connection: "+e);
            out.println("//error Server: Could not create connection to listen");
        } finally {
            try {
                if(session != null) 
                    session.close();
                if(conn != null)
                    conn.close();
            } catch(JMSException e) {
                System.err.println("Unable to close connection: "+e);
            }
        }
    }
    //listener to send the message to the client
    private static class IncomingListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            try {
                if(message instanceof TextMessage) {
                    System.out.println("//////////////Received TextMessage//////////////");
                    System.out.println("Text Message: "+((TextMessage)message).getText());
                    out.println(((TextMessage) message).getText());
                }
                else {
                    System.out.println("//////////////Received non-text Message//////////////");
                    System.out.println("Message: "+message);
                    out.println(message);
                }
            } catch (JMSException e) {
                System.out.println("Unable to receive text message: "+e);
                out.println("//error Server: Unable to receive Text Message");
            }
        }
    }
}
