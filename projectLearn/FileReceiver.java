package projectLearn;
import java.lang.Math;
import java.io.*;
import java.net.*;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.SynchronousQueue;

/*Protcol codes( first integer of the packet.) [code , info]
*	Value		Format(info)							Type
* 		0 		[file id,string filename size,filename]	request file with filename and id.	
*		1 		[file id,seq. no.]						request of file data packet
*		2		[file id,seq. no.,datasize,data]		to receive data packet.
*		3		[file id]								stop file id.					
*/

public class FileReceiver{
	private DatagramSocket socket;	//	socket of connection.
	private int packetsize;      	//	size of data in packets(in bytes).
	private String filename;		//	name of file to send.
	private int fileid;	  			//	id of file to send(This will indicate to client the file to which data beints).
	private int filesize;	  		//	file size in terms of sequence numbers.
	private SocketAddress remote;	//	remote address to send file with.
	private Queue<Integer> received;//	Queue to store sequence numbers of packets received.
	private BitSet acknowledged;// set of sequence number of received packets.
	private RandomAccessFile file;	//	file to be received.
	
	final public int PACKET_REQUEST = 1;
	final public int DATA_PACKET = 2;
	final public int STOP = 3;
	
	/**Constructor
	*@param DatagramSocket	soc		socket for host.
	*@param SocketAddress	rem		socket address of remote device.
	*@param String			fname	name of file to receive.
	*@param int				fid 	id of file to receive.
	*/
	FileReceiver(DatagramSocket soc,SocketAddress rem,String fname,int fid,int fsize)	throws FileNotFoundException{
		socket = soc;
		remote = rem;
		filename = fname;
		fileid = fid;
		filesize = fsize;
		
		packetsize = 8000;//Approx 8KB data in 1 packet.
		received = new LinkedList<Integer>();
		acknowledged = new BitSet(fsize);
		file = new RandomAccessFile(filename,"rw");
	}
	
	/**dettaches the header [fileid ,sequence number of packet data in original file].
	*
	*@return int	position of data in original file per packet size. -1 if fileid does not match. data is modified to have no header.
	*
	*@param  byte[]	data	data with header.
	*/
	public int dettachHeader(byte[] data){
		int seqno = -1;
		byte[] output = new byte[packetsize];
		try(ByteArrayInputStream bin = new ByteArrayInputStream(data)){
			try(DataInputStream in = new DataInputStream(bin)){
				if(fileid == in.readInt()){
					seqno = in.readInt();
					in.read(output);
				}
			}catch(IOException e){
				e.printStackTrace();
			}finally{}
			data = output;
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return seqno;
	}
	
	/**Receives packets
	*@return Thread thread which sends data as packets through socket.
	*/
	public Thread receiveDataPackets(){
		Thread t = new Thread(){
			public void run(){
				//send request for data
				for(int i=0;i<Math.ceil((double)filesize/packetsize);i++){
					while(true){
						sendDataRequest(i);
						byte[] pdata = new byte[packetsize+100];
						DatagramPacket dp = new DatagramPacket(pdata,pdata.length);
						try{
							socket.receive(dp);
							System.out.println("Something received from "+dp.getSocketAddress());
							try(ByteArrayInputStream bis = new ByteArrayInputStream(dp.getData(),dp.getOffset(),dp.getLength())){
								try(DataInputStream in = new DataInputStream(bis)){
									int pcode = in.readInt();
									int fid = in.readInt();
									int seqno = in.readInt();
									int dlen = in.readInt();
									byte[] data = new byte[dlen];
									in.read(data);
									file.seek(seqno*packetsize);
									file.write(data,0,dlen);
									System.out.println("received sequence number: "+seqno);
									break;
								}catch(IOException e){
								}finally{}
							}catch(IOException e){
								e.printStackTrace();
							}finally{}
						}catch(IOException ex){
						}
					}
				}
				//send stop 
				byte[] pdata={0};
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
					try(DataOutputStream out = new DataOutputStream(bos)){
						out.writeInt(STOP); // protocol code for data packets.
						out.writeInt(fileid);// file id
					}catch(IOException e){
						e.printStackTrace();
					}finally{}
					pdata = bos.toByteArray();
				}catch(IOException ex){
					ex.printStackTrace();
				}
				System.out.println("sending request to stop");
				DatagramPacket dp = new DatagramPacket(pdata,pdata.length,remote);
				try{
					socket.send(dp);
				}catch(IOException e){
					e.printStackTrace();
				}finally{}
				//file close
				try{
					file.close();
					//socket.close();
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		};
		return t;
	}
	
	/**send request for data packet at a given sequence number.
	*/
	public void sendDataRequest(int seqno){
		byte[] pdata={0};
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
			try(DataOutputStream out = new DataOutputStream(bos)){
				out.writeInt(PACKET_REQUEST); // protocol code for data packets.
				out.writeInt(fileid);// file id
				out.writeInt(seqno);// sequence number of data.
			}catch(IOException e){
				e.printStackTrace();
			}finally{}
			pdata = bos.toByteArray();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		//System.out.println(seqno+":"+new String(data));
		DatagramPacket dp = new DatagramPacket(pdata,pdata.length,remote);
		try{
			socket.send(dp);
			System.out.println("sending request for data at sequence number:"+seqno);
		}catch(IOException e){
			e.printStackTrace();
		}finally{}
	}
	
	public static void main(String[] args){
		try{
			InetAddress myip = InetAddress.getLocalHost();
			DatagramSocket soc = new DatagramSocket(2024,myip);
			soc.setSoTimeout(10000);
			System.out.println("Client address: "+soc.getLocalSocketAddress());
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String saddr = br.readLine();
			int sport = Integer.parseInt(br.readLine());
			SocketAddress server = new InetSocketAddress(saddr,sport);
			String filename = br.readLine();
			int fileid = 1;
			int fsize = Integer.parseInt(br.readLine());
			FileReceiver fs = new FileReceiver(soc,server,filename,fileid,fsize);
			System.out.println("Server address: "+server);
			Thread fst = fs.receiveDataPackets();
			fst.start();
			try{
				fst.join();
			}catch(InterruptedException e){
			}
			soc.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}