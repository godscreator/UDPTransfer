package projectLearn;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import projectLearn.FileTransferSystem;

public class peerClient implements Runnable
{
	String ServerAddress;
	int ServerPort;
	String Request;
	public peerClient(String Request)
	{
		this.Request = Request;
	}
	public void sendRequest(OutputStream dout,String request) throws IOException
	{
		for(int i=0;i<request.length();i++)
		{
			dout.write((char)request.charAt(i));
		}
		dout.write('\n');
		dout.flush();
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
						FileTransferSystem.readFile(in,filename);
					}catch(IOException e){}
				}
				else
				{
					System.out.println("File "+filename+" not read");
				}
			}
			else if(operation.equals("write"))
			{
				if(filename!=null)
				{
					try{
						FileTransferSystem.writeFile(filename,out);
					}catch(IOException e){}
				}
				else
				{
					System.out.println("File "+filename+" not written");
				}
			}
			else
			{
				System.out.println("Unkown operation.");
			}
		}else
		{
			System.out.println("No request recieved.");
		}		
	}
	@Override
	public void run()
	{
		String[] temp = Request.split(" ",3)[2].split(":",2);
		ServerAddress = temp[0];
		ServerPort = Integer.parseInt(temp[1]);
		
		//connect to the server.
		try(Socket socket = new Socket(this.ServerAddress,this.ServerPort)) {
			//socket.setSoTimeout(15000);
			System.out.println("Connected to server:");
			InputStream in = socket.getInputStream();
			//DataInputStream din = new DataInputStream(in);
			OutputStream out = socket.getOutputStream();
			//DataOutputStream dout = new DataOutputStream(out);
			this.sendRequest(out,Request);
			this.Response(Request,in,out);
			System.out.println("disconnected from server.");
		} catch (IOException ex) {
			System.err.println(ex);
		} 
	}
	public static void main(String[] args)
	{
		String request="";
		System.out.println("Enter a request:");
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			request = br.readLine();
		}catch(IOException e){}
		peerClient pc = new peerClient(request);
		Thread pc_t = new Thread(pc);
		pc_t.start();
		try{
			pc_t.join();
		}catch(InterruptedException e){}
	}
}