package projectLearn;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import projectLearn.FileDataBase;

public class MyServerClientHandler implements Runnable
{
	FileDataBase DB;
	Socket connection;
	public MyServerClientHandler(FileDataBase DB,Socket connection)
	{
		this.DB = DB;
		this.connection = connection;
	}
	public String readRequest(DataInputStream din) throws IOException
	{
		int i;
		StringBuilder request_b = new StringBuilder();
		while((i=din.read())!=-1 && i!=(int)'\n')
		{
			request_b.append((char)i);
		}
		return request_b.toString();
	}
	public void sendResponse(DataOutputStream dout,String response) throws IOException
	{
		for(int i=0;i<response.length();i++)
		{
			dout.write((char)response.charAt(i));
		}
		dout.write('\n');
		dout.flush();
	}
	public String execRequest(String Request)
	{
		if(Request!=null)
		{
			//System.out.println("REQUEST:"+req+".");
			//execute request from client.
			StringBuilder resp = new StringBuilder();//reponse of server to client request.
			boolean stat;
			String Response="";
			StringTokenizer stok;
			String operation="";
			String[] arg=new String[2];
			
			//parsing the request.
			stok = new StringTokenizer(Request," "); 
			if(stok.hasMoreTokens()){     // extract operation.
				operation = stok.nextToken();
				//System.out.println("OPERATION:"+operation+".");
			}
			for(int i=0;i<arg.length;i++){// extract arguments.                        
				if(stok.hasMoreTokens()){
					arg[i] = stok.nextToken();
					//System.out.println("ARGUMENT["+i+"]:"+arg[i]+".");
				}else break;
			}
			switch(operation)
			{
				case "join":
					this.DB.join(arg[0]);
					resp.append(arg[0]+" joined");
					break;
				case "leave":
					this.DB.leave(arg[0]);
					resp.append(arg[0]+" left");
					break;
				case "addfile":
					this.DB.addFile(arg[1],arg[0]);
					resp.append("file "+arg[0]+" added by "+arg[1]+".");
					break;
				case "removefile":
					resp.append("file "+arg[0]+(this.DB.removeFile(arg[1],arg[0])?"":" not")+" removed by "+arg[1]+".");
					break;
				case "searchfile":
					resp.append("file "+arg[1]+" is"+(this.DB.searchFile(arg[0],arg[1])?"":" not")+" shared by "+arg[0]+".");
					break;
				case "searchclient":
					resp.append(this.DB.searchClient(arg[0]));
					break;
				case "searchall":
					resp.append(this.DB.searchAll());
					break;
				default:
					resp.append("Unkown operation");
			}
			resp.append("\nEOR");//pass an end of response.
			return resp.toString();
		}else
		{
			return "No request recieved.";
		}		
	}
	@Override
	public void run()
	{			
		//loop to accept connection from clients
		try{
			InputStream in = this.connection.getInputStream();
			DataInputStream din = new DataInputStream(in);
			OutputStream out = this.connection.getOutputStream();
			DataOutputStream dout = new DataOutputStream(out);
			try{
				while(connection.isConnected() && ! connection.isClosed()){
					String request = this.readRequest(din);
					if(request.equals("exit"))break;
					String response = "ERROR EOR";
					if(!request.equals(""))
					{
						//System.out.println("REQUEST:"+request);
						synchronized(this.DB){
							response = execRequest(request);
						}
						//System.out.println("RESPONSE:"+response);
						this.sendResponse(dout,response);
					}
				}
			}catch(IOException e){
				System.out.println("ERROR:IO");
			}catch(Exception e){
				System.out.println("ERROR:"+e);
			}finally{
				try{
					connection.close();
				}catch(IOException e){}
			}
		}catch (IOException ex){
			System.out.println("ERROR:IO");
		}
	}
	public static void main(String[] args){
	}
		
}