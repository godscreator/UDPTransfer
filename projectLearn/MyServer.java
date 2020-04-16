package projectLearn;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import projectLearn.FileDataBase;
import projectLearn.MyServerClientHandler;

public class MyServer
{
	public static final int DEFAULT_PORT = 13;
	FileDataBase DB;
	public MyServer()
	{
		this.DB = new FileDataBase();
	}
	public void run()
	{
		// creating a new server socket bound to DEFAULT_PORT .
		try (ServerSocket server = new ServerSocket(DEFAULT_PORT)) {
		
			// Show details of server socket
			System.out.println("SERVER DETAILS: "+server.toString());
				
			//loop to accept connection from clients
			int maxConnection = 5;
			while (maxConnection>0) {
				try{
					Socket connection = server.accept();
					//System.out.println("CLIENT DETAILS: "+connection.toString());
					//System.out.println("CLIENT Address: "+connection.getRemoteSocketAddress().toString());
					MyServerClientHandler ch = new MyServerClientHandler(this.DB,connection);
					new Thread(ch).start();
				} catch (IOException ex) {
					System.out.println("ERROR:IO");
				}
				maxConnection--;
			}
			
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
	public static void main(String[] args){
		MyServer ms = new MyServer();
		ms.run();
	}
		
}