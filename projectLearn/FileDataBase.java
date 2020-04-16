package projectLearn;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.*;
import java.util.StringTokenizer;

public class FileDataBase
{
	//database: key:clientname (string) value: set of filenames(string) that the client has.
	public Hashtable<String,HashSet<String>> DB;

	public FileDataBase()
	{
		DB = new Hashtable<String,HashSet<String>>();
	}
	
	public void join(String clientname)
	{
		// add a empty file list under the client 
		HashSet<String> fileList = new HashSet<String>();
		DB.put(clientname,fileList);
	}
	
	public void leave(String clientname)
	{
		// remove all files under the client 
		DB.remove(clientname);
	}
	
	public void addFile(String clientname,String filename)
	{
		//adds filename file under client clientname.
		
		if(DB.containsKey(clientname))
		{
			HashSet<String> fileList = DB.get(clientname);
			fileList.add(filename);
		}
		else
		{
			HashSet<String> fileList = new HashSet<String>();
			fileList.add(filename);
			DB.put(clientname,fileList);
		}
	}
	public boolean removeFile(String clientname,String filename)
	{
		//removes filename file under client clientname. Returns true if it is successful.
		
		if(DB.containsKey(clientname))
		{
			HashSet<String> fileList = DB.get(clientname);
			return fileList.remove(filename);
		}
		else
		{
			return false;
		}
	}
	public boolean searchFile(String clientname,String filename)
	{
		//search filename file under client clientname. Returns true if it is successful.
		
		if(DB.containsKey(clientname))
		{
			HashSet<String> fileList = DB.get(clientname);
			return fileList.contains(filename);
		}
		else
		{
			return false;
		}
	}
	public String searchClient(String clientname)
	{
		//searches all file under client clientname. Returns filenames(string) available.
		if(DB.containsKey(clientname))
		{
			HashSet<String> fileList = DB.get(clientname);
			String[] files = fileList.toArray(new String[0]);
			
			StringBuilder toReturn = new StringBuilder();
			toReturn.append("Files:");
			if(files.length==0)
				toReturn.append("\n\tNo files to share.\n\t");
			for(int i = 0;i<files.length;i++)
				toReturn.append(files[i]+"\n\t");
			toReturn.append("\n");
			return toReturn.toString();
		}
		else
		{
			return "";
		}
	}
	public String searchAll()
	{
		//searches all files for all  clients. Returns array of filenames(string) seperate by line feed ,available.
		StringBuilder toReturn = new StringBuilder();
		String[] files;
		toReturn.append("All files: \n");
		for(String cn:Collections.list(DB.keys()))
		{
			toReturn.append(cn+"\n\t");
			files = DB.get(cn).toArray(new String[0]);
			if(files.length==0)
				toReturn.append("No files to share.\n\t");
			for(int i = 0;i<files.length;i++)
				toReturn.append(files[i]+"\n\t");
			toReturn.append("\n");
		}
		return toReturn.toString();
	}
	
	public static void main(String[] args){
		//Test code:
		FileDataBase DB = new FileDataBase();
		boolean stat,run=true;
		String Response="",Request="";
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		StringTokenizer stok;
		String operation="";String[] arg=new String[2];
		while(run)
		{
			System.out.println("Enter a request:");
			try{
				Request = br.readLine();
			}catch(IOException e){}
			//parsing the request.
			stok = new StringTokenizer(Request," "); 
			if(stok.hasMoreTokens()){     // extract operation.
				operation = stok.nextToken();
				System.out.println("OPERATION:"+operation+".");
			}
			for(int i=0;i<arg.length;i++){// extract arguments.                        
				if(stok.hasMoreTokens()){
					arg[i] = stok.nextToken();
					System.out.println("ARGUMENT["+i+"]:"+arg[i]+".");
				}else break;
			}
			switch(operation)
			{
				case "exit":
					run = false;
					break;
				case "join":
					DB.join(arg[0]);
					System.out.println(arg[0]+" joined");
					break;
				case "leave":
					DB.leave(arg[0]);
					System.out.println(arg[0]+" left");
					break;
				case "addfile":
					DB.addFile(arg[0],arg[1]);
					System.out.println("file "+arg[1]+" added by "+arg[0]+".");
					break;
				case "removefile":
					stat = DB.removeFile(arg[0],arg[1]);
					System.out.println("file "+arg[1]+(stat?"":" not")+" removed by "+arg[0]+".");
					break;
				case "searchfile":
					stat = DB.searchFile(arg[0],arg[1]);
					System.out.println("file "+arg[1]+(stat?"":" not")+" is shared by "+arg[0]+".");
					break;
				case "searchclient":
					Response = DB.searchClient(arg[0]);
					System.out.println(Response);
					break;
				case "searchall":
					Response = DB.searchAll();
					System.out.println(Response);
					break;
				default:
					System.out.println("Unkown operation");
			}
		}
	}
		
}