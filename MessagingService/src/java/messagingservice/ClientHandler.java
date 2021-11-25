/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messagingservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 *
 * @author Vorno
 */
public class ClientHandler implements Runnable{
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private static ConnectionFactory connectionFactory;
    private static Queue queue;
    private WriteToQueue writeMessage;
    private ReceiveFromQueue receiveMessage;
    private boolean isAlive = true;
    private static boolean writeIsBusy = false;
    private static boolean receiveIsBusy = false;
    
    
    public ClientHandler(Socket clientSocket, ConnectionFactory connectionFactory, Queue queue) throws IOException {
        this.client = clientSocket;
        this.connectionFactory = connectionFactory;
        this.queue = queue;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
    }
    
    //client handler thread which allows for the client to communicate what they want to do (receive or write messages)
    @Override
    public void run() {
        try {
            while(isAlive) {
                String request = in.readLine();
                System.out.println(request);
                //receive messages
                if(request.contains("//listen")) {  //consume from queue
                    if(!receiveIsBusy) {
                        receiveIsBusy = true;
                        receiveMessage = new ReceiveFromQueue(in, out, connectionFactory, queue);
                        receiveIsBusy = false;
                    }
                    else {
                        out.println("//busy");
                    }
                }
                //create messages
                else if(request.contains("//write")) { //add to queue
                    if(!writeIsBusy) {
                        writeIsBusy = true;
                        writeMessage = new WriteToQueue(in, out, connectionFactory, queue);
                        writeIsBusy = false;
                    }
                    else
                        out.println("//busy");
                }
                
            }
        } catch(IOException e) {
            System.err.println("IO Exception in client handler: "+e);
            out.println("//error Error with request");
        } 
    }
    
    public void stop() {
        isAlive = false;
    }
}
        