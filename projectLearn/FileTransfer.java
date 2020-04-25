package projectLearn;
import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.*;
import projectLearn.FileSender;
import projectLearn.FileReceiver;
import projectLearn.FileInfo;

/*Protcol codes( first integer of the packet.) [code , info]
*	Value		Format(info)							Type
* 		0 		[file id,string filename size,filename]	request file with filename and id.							-
*		1 		[file id,seq. no.]						request of file data packet
*		2		[file id,seq. no.,datasize,data]		to send data packet.
*		3		[file id]								stop file id.					
*/

/*Request types
* 1> addfile fileid filename [filepath]
* 2> removefile filename
* 3> download clientaddress clientport fileid filename filesize
* 4> getshared clientaddress clientport	
* 5> exit	
*/
public class FileTransfer
{
	DatagramSocket Sendersocket;
	DatagramSocket Receiversocket;
	DatagramSocket socket;
	Hashtable<String,FileInfo> filelist; // key : filename , value: file info(file id, file size, file path)
	
	ExecutorService service;
	ReentrantReadWriteLock rwl;
	boolean serverStatus;
	
	final static int TIMEOUT = 10000;
	final static String REQUEST_TYPES = "Request types	"+
	"\n1> addfile fileid filename [filepath]"+
	"\n2> removefile filename"+
	"\n3> download clientaddress clientport fileid filename filesize"+
	"\n4> getshared clientaddress clientport"+
	"\n5> exit";
	
	public FileTransfer(InetAddress addr,int port,int sport,int rport) throws SocketException,UnknownHostException{
		socket = new DatagramSocket(port,addr);
		Sendersocket = new DatagramSocket(sport,addr);
		Receiversocket = new DatagramSocket(rport,addr);
		//socket.setSoTimeout(TIMEOUT);
		System.out.println("Server address: "+socket.getLocalSocketAddress());
		System.out.println("Sender address: "+Sendersocket.getLocalSocketAddress());
		System.out.println("Receiver address: "+Receiversocket.getLocalSocketAddress());
		filelist = new Hashtable<String,FileInfo>();
		service = Executors.newFixedThreadPool(10);
		rwl = new ReentrantReadWriteLock();
		serverStatus = false;
	}
	
	/** thread for sending file
	*/
	public Runnable servefiles(){
		serverStatus = true;
		Thread t = new Thread(){
			public void run(){
				//rwl.readLock().lock();
				while(serverStatus){
					//rwl.readLock().unlock();
					//if (Thread.currentThread().isInterrupted()) break;
					//receive a request.
					byte[] data = new byte[8192];
					DatagramPacket dp = new DatagramPacket(data,data.length);
					try{
						socket.receive(dp);
					}catch(IOException e){}
					//parse the request
					
					try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(dp.getData(),dp.getOffset(),dp.getLength()))){
						int pcode = in.readInt();// protocol code
						int fid = in.readInt();  // file id
						int fnsize = in.readInt();	 // filename size
						byte[] fname = new byte[fnsize];
						in.read(fname,0,fname.length); //filename
						String filename= new String(fname);
						System.out.println("pcode:"+pcode+" fid:"+fid+" filename:"+filename);
						//rwl.readLock().lock();
						if(pcode == 0 && filelist.containsKey(filename)){
							FileInfo fi = filelist.get(filename);
							if(fi.fileid == fid){
								FileSender fs = new FileSender(Sendersocket,dp.getSocketAddress(),filename,fid);
								service.submit(fs.sendPackets());
								System.out.println("File asked:"+filename);
							}							
						}
						//rwl.readLock().unlock();
					}catch(IOException e){
						//e.printStackTrace();
					}finally{}
					//rwl.readLock().lock();
				}
				//rwl.readLock().unlock();
				System.out.println("server exiting..");
			}
		};
		return t;
	}
	
	/** add file info to file list
	*@return boolean true if file added to filelist else false
	*
	*@param int 	fid 	file id
	*@param String	fname	file name
	*@param String	fpath	file path
	*/
	public boolean addfile(int fid,String fname,String fpath){
		int fsize;
		try{
			RandomAccessFile file = new RandomAccessFile(fpath+fname,"r");
			try{
			fsize = (int)file.length();
			file.close();
			System.out.println("Filesize:"+fsize);
			}catch(IOException ex){
				return false;
			}
		}catch(FileNotFoundException e){
			return false;
		}
		//rwl.writeLock().lock();
		FileInfo fi = new FileInfo(fid,fsize,fpath);
		filelist.put(fname,fi);
		//rwl.writeLock().unlock();
		return true;
	}
	
	/** remove file info to file list
	*@return boolean true if file removed from filelist else false
	*
	*@param String	fname	file name
	*/
	public boolean removefile(String fname){
		//rwl.writeLock().lock();
		if(filelist.containsKey(fname)){
			filelist.remove(fname);
			//rwl.writeLock().unlock();
			return true;
		}else{
			//rwl.writeLock().unlock();
			return false;
		}
	}
	
	/** get info of shared files from client.
	*
	*@param String	caddr	client address.
	*@param int		port	client port.
	*/
	public void getshared(String caddr,int port){
		
	}
	
	/** get file with file id and filename from client.
	*
	*@param String	caddr	client address.
	*@param int		port	client port.
	*@param int 	fileid	file id.
	*@param String	filename name of file
	*@param int		fsize	size of file in bytes.
	*/
	public void download(String caddr,int port,int rport,int fileid,String filename,int fsize){
		SocketAddress client = new InetSocketAddress(caddr,port);
		byte[] pdata={0};
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
			try(DataOutputStream out = new DataOutputStream(bos)){
				out.writeInt(0); // protocol code .
				out.writeInt(fileid);
				byte[] fname = filename.getBytes();
				out.writeInt(fname.length);
				out.write(fname,0,fname.length);
			}catch(IOException e){
				e.printStackTrace();
			}finally{}
			pdata = bos.toByteArray();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		DatagramPacket dp = new DatagramPacket(pdata,pdata.length,client);
		try{
			Receiversocket.send(dp);
		}catch(IOException e){
			e.printStackTrace();
		}finally{}
		SocketAddress clientsender = new InetSocketAddress(caddr,rport);
		try{
			FileReceiver fr = new FileReceiver(Receiversocket,clientsender ,"downloaded"+filename,fileid,fsize);
			service.submit(fr.receiveDataPackets());
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
	
	/**execute request from user.
	*
	*@return String opcode for request
	*
	*@param String	request	request from user.
	*/
	public String execRequest(String request){
		String opcode = "ERROR";
		String[] parsedRequest = request.split(" ");
		boolean isDone=false;
		if(parsedRequest.length>0){
			opcode = parsedRequest[0];
			switch(opcode){
				case "addfile":
					if(parsedRequest.length==4){
						isDone = addfile(Integer.parseInt(parsedRequest[1]),parsedRequest[2],parsedRequest[3]);
					}else if(parsedRequest.length==3){
						isDone = addfile(Integer.parseInt(parsedRequest[1]),parsedRequest[2],"");
					}
					if(isDone){
						System.out.println("File added.");
					}else{
						System.out.println("File not added. Usage:addfile fileid filename [filepath]");
					}
					break;
				case "removefile":
					if(parsedRequest.length==2){
						isDone = removefile(parsedRequest[1]);
					}
					if(isDone){
						System.out.println("File removed.");
					}else{
						System.out.println("File not removed. Usage:removefile filename");
					}
					break;
				case "download":
					if(parsedRequest.length==7){
						download(parsedRequest[1],Integer.parseInt(parsedRequest[2]),Integer.parseInt(parsedRequest[3]),Integer.parseInt(parsedRequest[4]),parsedRequest[5],Integer.parseInt(parsedRequest[6]));
					}
					break;
				case "getshared":
					break;
				case "exit":
					break;
				default:
					System.out.println("Usage:"+REQUEST_TYPES);
			}
		}
		return opcode;
	}
	
	public static void main(String[] args)throws InterruptedException{
		int port,sport,rport;
		if(args.length==3){
			port=Integer.parseInt(args[0]);
			sport=Integer.parseInt(args[1]);
			rport=Integer.parseInt(args[2]);
		}else{
			port=2022;
			sport=2023;
			rport=2024;
		}
		try{
			FileTransfer ft = new FileTransfer(InetAddress.getLocalHost(),port,sport,rport);
			ft.service.submit(ft.servefiles());
			System.out.println("Usage:"+REQUEST_TYPES);
			String request="";
			while(true){
				try{
					System.out.println("Enter a request:");
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					request = br.readLine();
					if(ft.execRequest(request).equals("exit"))
						break;
				}catch(IOException e){}
			}
			//ft.rwl.writeLock().lock();
			ft.serverStatus = false;
			ft.socket.close();
			//ft.rwl.writeLock().lock();
			ft.service.shutdownNow();
			 if (!ft.service.awaitTermination(100, TimeUnit.MICROSECONDS)) {
				System.out.println("Still waiting...");
				System.exit(0);
			}
			System.out.println("Exiting normally...");
		}catch(SocketException e){
			e.printStackTrace();
		}
		catch(UnknownHostException e){
			e.printStackTrace();
		}
	}
}