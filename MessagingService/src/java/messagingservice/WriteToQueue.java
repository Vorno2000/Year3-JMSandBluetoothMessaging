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
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 *
 * @author Vorno
 */
public class WriteToQueue {
    private static BufferedReader in;
    private static PrintWriter out;
    private static ConnectionFactory connectionFactory;
    private static Queue queue;
    
    //constructor
    public WriteToQueue (BufferedReader in, PrintWriter out, ConnectionFactory connectionFactory, Queue queue) {
        this.in = in;
        this.out = out;
        this.connectionFactory = connectionFactory;
        this.queue = queue;
        writeToQueue();
    }
    //allows client to create messages - also establishes connection to connection factory
    public synchronized void writeToQueue() { //sender
        Connection conn;
        Session session;
        MessageProducer producer;
        TextMessage message;
        
        try {
            conn = connectionFactory.createConnection();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(queue);
            message = session.createTextMessage();
            
            out.println("//success");
            out.println("//success");
            
            String newMessage = in.readLine();
            if(newMessage.equals("//listen") || newMessage.equals("//write") || newMessage.equals("null")) {
                message.setText("//error");
            }
            else
                message.setText(newMessage);
        } catch (JMSException e) {
            System.err.println("Unable to open connection: "+e);
            out.println("//error Server: Error connecting to queue");
            return;
        } catch (IOException ex) {
            System.err.println("Unable to send message: "+ex);
            out.println("//error Server: Error sending message");
            return;
        } catch (NullPointerException err) {
            System.err.println("Null pointer exception: "+err);
            out.println("//error Server: Null pointer Exception");
            return;
        }
        //producer sends message and prints it out on the server log
        try {
            if(message.getText().equals("//error")) {
                System.out.println("//////////////Error with Message://////////////");
                System.out.println("//////////////Message not Sent//////////////");
            }
            else {
                System.out.println("//////////////Sending Message://////////////"+message);
                producer.send(message);
                System.out.println("//////////////Successfully sent Message//////////////");
            }
            out.println("//complete");
            
            session.close();
            conn.close();
            
        } catch (JMSException err) {
            System.err.println("Unable to set message: "+err);
            out.println("//error Server: Error sending message");
            try {
                session.close();
                conn.close();
            }catch(JMSException error) {
                System.err.println("Could not close connection: "+error);
            }
        } catch (NullPointerException nErr) {
            System.err.println("Null Pointer Exception: "+nErr);
        }
    }
}
