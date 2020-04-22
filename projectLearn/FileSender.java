package projectLearn;
import java.io.*;
import java.net.*;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*Protcol codes( first integer of the packet.)
*	Value		Format			Type
* 		0 		-				-
*		1 		[fi				request of file 
*/

public class FileSender{
	private DatagramSocket socket;	//	socket of connection.
	private int packetsize;      	//	size of data in packets(in bytes).
	private String filename;		//	name of file to send.
	private int fileid;	  			//	id of file to send(This will indicate to client the file to which data beints).
	private int filesize;			//	size of file to send.
	private SocketAddress remote;	//	remote address to send file with.
	
	private BitSet acknowledged; 	//	set of indexes of acknowledged packets.
	private Queue<Integer> toSend;		//	Queue to store indexes of packets to send.
	private RandomAccessFile file;	//	file to be transfered.
	
	private ReentrantReadWriteLock lock;	// lock to syncronize between thread.
	
	/**Constructor
	*@param DatagramSocket	soc		socket for host.
	*@param SocketAddress	rem		socket address of remote device.
	*@param String			fname	name of file to send.
	*@param int				fid 	hash of name of file to send.
	*@param int				fsize	size of file to send.
	*/
	FileSender(DatagramSocket soc,SocketAddress rem,String fname,int fid,int fsize)	throws FileNotFoundException{
		socket = soc;
		remote = rem;
		filename = fname;
		fileid = fid;
		filesize = fsize;
		
		
		packetsize = 8000;//Approx 8KB data in 1 packet.
		acknowledged = new BitSet(filesize);
		toSend = new LinkedList<Integer>();
		file = new RandomAccessFile(filename,"r");
		
		for(int i = 0;i<filesize/packetsize;i++)
			toSend.add(i);
	}
	
	/**Attaches a header [fileid ,index of packet data in original file].
	*
	*@return byte[] byte array with the header attached to data.
	*
	*@param  byte[]	data	data to attach header to.
	*@param  int 	index	index is position of data in original file.
	*/
	public byte[] attachHeader(byte[] data,int index){
		byte[] output={0};
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
			try(DataOutputStream out = new DataOutputStream(bos)){
				out.writeInt(fileid);
				out.writeInt(index);
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
	
	/**Sends packets
	*@return Thread thread which sends data as packets through socket.
	*/
	public Thread sendPackets(){
		Thread t = new Thread(){
			public void run(){
				lock.readLock().lock();
				while(toSend.size()>0){
					lock.readLock().unlock();
					boolean istosend = false;
					int index = -1;
					//check if the given index is acknowledged by client.
					lock.readLock().lock();
					istosend = !acknowledged.get(toSend.peek());
					lock.readLock().unlock();
					// if not acknowledged then send packet.
					if(istosend && index>-1){
						//deque head and enqueue it to back.
						lock.writeLock().lock();
						index = toSend.remove();
						toSend.add(index);
						lock.writeLock().unlock();
						//read data from file to send.
						byte[] data = new byte[packetsize];
						try{
							file.seek(index*packetsize);
							file.read(data);
							//send packets.
							byte[] pdata = attachHeader(data,index);
							//System.out.println(index+":"+new String(data));
							DatagramPacket dp = new DatagramPacket(pdata,pdata.length,remote);
							try{
								socket.send(dp);
							}catch(IOException e){
								e.printStackTrace();
							}finally{}
						}catch(IOException ex){
							ex.printStackTrace();
						}finally{}
					}
					lock.readLock().lock();
				}
				lock.readLock().unlock();
			}
		};
		t.start();
		return t;
	}
	
	/**receives acknowlegment from receiver.
	*@return Thread thread receive acknowlegment through socket.
	*/
	public Thread receiveAcknowledgement(){
		Thread t = new Thread(){
			public void run(){
				lock.readLock().lock();
				while(toSend.size()>0){
					lock.readLock().unlock();
					byte[] data = new byte[1001*Integer.BYTES];
					DatagramPacket dp = new DatagramPacket(data,data.length);
					try{
						socket.receive(dp);
						if(dp.getSocketAddress()!=rem)
							continue;
						DataInputStream in = new DataInputStream(new ByteArrayInputStream(dp.getData(),dp.getOffset(),dp.getLength()));
						try{
							int fid = in.readInt();
							if(fid == fileid){
								for(int i = 0;i<1000;i++){
									int ind = in.readInt();
									lock.readLock().lock();
									acknowledged.set(ind);
									lock.readLock().unlock();
								}
							}
						}catch(IOException e){
							e.printStackTrace();
						}finally{}
					}catch(IOException ex){
						ex.printStackTrace();
					}finally{}
					lock.readLock().lock();
				}
				lock.readLock().unlock();
			}
		};
		t.start();
		return t;
	}
	
	public static void main(String[] args){
	}
}