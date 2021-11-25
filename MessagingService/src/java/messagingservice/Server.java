/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messagingservice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

/**
 *
 * @author Vorno
 */
public class Server {
    private static ArrayList<Thread> clients = new ArrayList<>();
    @Resource(mappedName="jms/ConnectionFactory")
        private static ConnectionFactory connectionFactory;
    @Resource(mappedName="jms/MessageQueue") private static Queue queue;
   
    //main class which starts all the threads
    public static void main(String[] args) throws IOException{
        ServerSocket listener = new ServerSocket(5000);
        System.out.println("//////////////Server has Started://////////////");
        while(true) {
            Socket client = listener.accept();
            System.out.println("//////////////A client has Connected://////////////");
            ClientHandler clientThread = new ClientHandler(client, connectionFactory, queue);
            Thread t = new Thread(clientThread);
            t.start();
            clients.add(t);
        }
    }
}
