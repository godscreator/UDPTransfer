package projectLearn;
import java.io.*;
import java.net.*;
import java.lang.Math;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*Protcol codes( first integer of the packet.) [code , info]
*	Value		Format(info)						Type
* 		0 		-									-
*		1 		[file id,seq. no.]					request of file data packet
*		2		[file id,seq. no.,data]				to send data packet.
*		3		[file id]							stop file id.					
*/

public class FileSender{
	private DatagramSocket socket;	//	socket of connection.
	private int packetsize;      	//	size of data in packets(in bytes).
	private String filename;		//	name of file to send.
	private int fileid;	  			//	id of file to send(This will indicate to client the file to which data beints).
	private int filesize;			//	size of file to send.
	private SocketAddress remote;	//	remote address to send file with.
	
	private RandomAccessFile file;	//	file to be transfered.
	
	final public int PACKET_REQUEST = 1;
	final public int DATA_PACKET = 2;
	final public int STOP = 3;
	
	/**Constructor
	*@param DatagramSocket	soc		socket for host.
	*@param SocketAddress	rem		socket address of remote device.
	*@param String			fname	name of file to send.
	*@param int				fid 	id of file to send.
	*@param int				fsize	size of file to send.
	*/
	FileSender(DatagramSocket soc,SocketAddress rem,String fname,int fid)	throws FileNotFoundException,IOException{
		socket = soc;
		remote = rem;
		filename = fname;
		fileid = fid;
		
		
		packetsize = 8000;//Approx 8KB data in 1 packet.
		file = new RandomAccessFile(filename,"r");
		filesize = (int)file.length();
		System.out.println("filesize:" +filesize);
	}
	
	/**Attaches a header [fileid ,sequence number of packet data in original file].
	*
	*@return byte[] byte array with the header attached to data.
	*
	*@param  byte[]	data	data to attach header to.
	*@param  int 	seqno	seqno is position of data in original file.
	*/
	public byte[] attachDataHeader(byte[] data,int seqno){
		byte[] output={0};
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
			try(DataOutputStream out = new DataOutputStream(bos)){
				out.writeInt(DATA_PACKET); // protocol code for data packets.
				out.writeInt(fileid);
				out.writeInt(seqno);
				out.write(data,0,data.length);
			}catch(IOException e){
				e.printStackTrace();
			}finally{}
			output = bos.toByteArray();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return output;
	}
	
	/**Sends a packet
	*@param	int	seqno	sequence number of packet.
	*/
	public void sendPacket(int seqno){
		//read data from file to send.
		byte[] data;
		if((seqno+1)*packetsize<=filesize)
			data = new byte[packetsize];
		else
			data = new byte[filesize-seqno*packetsize];
		try{
			file.seek(seqno*packetsize);
			file.read(data);
			//send packets.
			byte[] pdata = attachDataHeader(data,seqno);
			System.out.println("sending packets at sequence number:"+seqno);
			DatagramPacket dp = new DatagramPacket(pdata,pdata.length,remote);
			try{
				socket.send(dp);
			}catch(IOException e){
				e.printStackTrace();
			}finally{}
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	/**Sends packets as requested by client.
	*@return Thread thread which sends data as packets through socket.
	*/
	public Thread sendPackets(){
		Thread t = new Thread(){
			public void run(){
				boolean run = true;
				while(run){
					byte[] data = new byte[3*Integer.BYTES];
					DatagramPacket dp = new DatagramPacket(data,data.length);
					try{
						socket.receive(dp);
						System.out.println("Something received from "+dp.getSocketAddress());
						//if(!dp.getSocketAddress().equals(remote))
						//	continue;
						DataInputStream in = new DataInputStream(new ByteArrayInputStream(dp.getData(),dp.getOffset(),dp.getLength()));
						try{
							int pcode = in.readInt();// protocol code
							int fid = in.readInt();  // file id
							if(pcode == STOP){
								run = false;
								file.close();
								break;
							}
							int sqn = in.readInt();	 // sequence number
							System.out.println("pcode:"+pcode+" fid:"+fid+" sqn:"+sqn+"Math.ceil(filesize/packetsize):"+Math.ceil((double)filesize/packetsize));
							if(pcode == PACKET_REQUEST && fid == fileid && sqn>=0 && sqn<Math.ceil((double)filesize/packetsize)){
								sendPacket(sqn);
							}
						}catch(IOException e){
							e.printStackTrace();
						}finally{}
					}catch(IOException ex){
							ex.printStackTrace();
					}finally{}
				}
			}
		};
		t.start();
		return t;
	}
	
	public static void main(String[] args){
		try{
			InetAddress myip = InetAddress.getLocalHost();
			DatagramSocket soc = new DatagramSocket(2023,myip);
			soc.setSoTimeout(10000);
			System.out.println("Server address: "+soc.getLocalSocketAddress());
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String caddr = br.readLine();
			int cport = Integer.parseInt(br.readLine());
			SocketAddress client = new InetSocketAddress(caddr,cport);
			String filename = "data.txt";
			int fileid = 1;
			FileSender fs = new FileSender(soc,client,filename,fileid);
			System.out.println("Client address: "+client);
			Thread fst = fs.sendPackets();
			try{
				fst.join();
			}catch(InterruptedException e){
			}
			soc.close();
		}catch(IOException e){
		}
	}
}