package projectLearn;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import projectLearn.FileTransferSystem;

public class peerServer implements Runnable
{
	int ServerPort;
	ServerSocket server;
	private volatile boolean isAccepting = true;
	public peerServer()
	{
		this.ServerPort = 0;
	}
	public String readRequest(InputStream din) throws IOException
	{
		int i;
		StringBuilder request_b = new StringBuilder();
		while((i=din.read())!=-1 && i!=(int)'\n')
		{
			request_b.append((char)i);
		}
		return request_b.toString();
	}
	public void Response(String req,InputStream in,OutputStream out) throws IOException
	{
		//System.out.println("REQUEST:"+req+".");
		//execute request from client.
		if(req!=null)
		{
			StringTokenizer stok = new StringTokenizer(req," ");
			String operation=null,filename=null;
			if(stok.hasMoreTokens())
			{
				operation = stok.nextToken();
				//System.out.println("OPERATION:"+operation+".");
			}
			if(stok.hasMoreTokens())
			{
				filename = stok.nextToken();
				//System.out.println("FILENAME:"+filename+".");
			}
			if(operation.equals("read"))
			{
				if(filename!=null)
				{
					try{
						FileTransferSystem.writeFile(filename,out);
					}catch(IOException e){}
				}
				else
				{
					//System.out.println("File "+filename+" not read");
				}
			}
			else if(operation.equals("write"))
			{
				if(filename!=null)
				{
					try{
						FileTransferSystem.readFile(in,filename);
					}catch(IOException e){}
				}
				else
				{
					//System.out.println("File "+filename+" not written");
				}
			}
			else
			{
				//System.out.println("Unkown operation.");
			}
		}else
		{
			//System.out.println("No request recieved.");
		}		
	}
	@Override
	public void run()
	{
		try{
			server = new ServerSocket(0);
			ServerPort = server.getLocalPort();
			
			//System.out.println("PEER SERVER DETAILS:"+server.toString());
			
			//loop to accept connection from clients
			
			while (isAccepting) {
				try (Socket connection = server.accept()) {
					//System.out.println("CLIENT DETAILS: "+connection.toString());
					//System.out.println("CLIENT Address: "+connection.getRemoteSocketAddress().toString());
					InputStream in = connection.getInputStream();
					//DataInputStream din = new DataInputStream(in);
					OutputStream out = connection.getOutputStream();
					//DataOutputStream dout = new DataOutputStream(out);
					try{
						String request = this.readRequest(in);
						//System.out.println("REQUEST:"+request);
						this.Response(request,in,out);
						//System.out.println("RESPONSE:"+response);
					}catch(IOException e)
					{
						//System.out.println("ERROR:IO");
					}
					//System.out.println("closing connection");
				} catch (IOException ex) {
					//System.out.println("ERROR:IO");
				}
				
			}
		} catch (IOException ex) {
			//System.out.println(ex);
		}finally{
			if(server!=null)
			{
				try{
					server.close();
				}catch(IOException e){}
			}
		}
		//System.out.println("Server is closed");
	}
	
	public void stopServer()  throws IOException
	{
		isAccepting = false;
		server.close();
	}
	public static void main(String[] args)
	{
		String request="";
		//setting up a peer server
		peerServer ps = new peerServer();
		Thread ps_t = new Thread(ps);
		ps_t.start();
		//while(ps.ServerPort==0);// wait to allocate a port for peer server.
		
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
				break;
			}
		}
		
		try{
		ps.stopServer();
		}catch(IOException e){}
		try{
		ps_t.join();
		}catch(InterruptedException e){}
	}
}