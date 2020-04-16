package projectLearn;
import java.io.*;
import java.net.*;
import projectLearn.peerServer;
import projectLearn.peerClient;

public class MyClient implements Runnable
{
	String ServerAddress;
	int ServerPort;
	public MyClient(String ServerAddr,int port)
	{
		this.ServerAddress = ServerAddr;
		this.ServerPort = port;
	}
	public String recieveResponse(DataInputStream din) throws IOException
	{
		int i=-1,j=-1,k=-1;
		StringBuilder response_b = new StringBuilder();
		while((i=din.read())!=-1 && !(k==(int)'E'&& j==(int)'O'&& i==(int)'R'))
		{
			response_b.append((char)i);
			k=j;
			j=i;
		}
		if(i>-1)
			response_b.append((char)i);
		return response_b.toString();
	}
	public void sendRequest(DataOutputStream dout,String request) throws IOException
	{
		for(int i=0;i<request.length();i++)
		{
			dout.write((char)request.charAt(i));
		}
		dout.write('\n');
		dout.flush();
	}
	@Override
	public void run()
	{
		String request="ERROR",response="ERROR";
		
		//setting up a peer server
		peerServer ps = new peerServer();
		Thread ps_t = new Thread(ps);
		ps_t.start();
		//while(ps.ServerPort==0);// wait to allocate a port for peer server.
		
		try(Socket socket = new Socket(this.ServerAddress,this.ServerPort)) {
			socket.setSoTimeout(15000);
			InputStream in = socket.getInputStream();
			DataInputStream din = new DataInputStream(in);
			OutputStream out = socket.getOutputStream();
			DataOutputStream dout = new DataOutputStream(out);
			
			while(true){
				//synchronized(System.in){ // Read request from user that is from command line.
				try{
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					request = br.readLine();
				}catch(IOException e){}
				//}
				if(request.equals("exit"))
				{
					try{
						ps.stopServer();
					}catch(IOException e){}
					try{
					ps_t.join();
					}catch(InterruptedException e){}
					break;
				}
				//synchronized(System.out){// Show the request from user.
				//System.out.println("REQUEST:"+request);
				//}
				if(request.startsWith("read")||request.startsWith("write")){
					peerClient pc = new peerClient(request);
					Thread pc_t = new Thread(pc);
					pc_t.start();
					try{
					pc_t.join();
					}catch(InterruptedException e){}
				}
				else{
					this.sendRequest(dout,request+" "+ps.server.getInetAddress().getHostAddress()+":"+ps.server.getLocalPort());
					response = this.recieveResponse(din);
					//synchronized(System.out){
					System.out.println("RESPONSE:"+response);
					//}
				}
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
		//closing the server.
		try{
		ps.stopServer();
		}catch(IOException e){}
		try{
		ps_t.join();
		}catch(InterruptedException e){}
	}
	public static void main(String[] args) throws IOException
	{
		
		String host = args.length > 0 ? args[0] : "0.0.0.0";
		int port = args.length > 1 ? Integer.parseInt(args[1]):13;
		MyClient mc = new MyClient(host,port);
		Thread mc_t = new Thread(mc);
		mc_t.start();
		try{
			mc_t.join();
		}catch(InterruptedException e){}
		System.out.println("Exiting..");
	}
}