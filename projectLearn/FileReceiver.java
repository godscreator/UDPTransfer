package projectLearn;
import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.SynchronousQueue;

public class FileReceiver{
	private DatagramSocket socket;	//	socket of connection.
	private int packetsize;      	//	size of data in packets(in bytes).
	private String filename;		//	name of file to send.
	private int fileid;	  			//	id of file to send(This will indicate to client the file to which data beints).
	private SocketAddress remote;	//	remote address to send file with.
	
	private SynchronousQueue<Integer> received;//	Queue to store indexes of packets received.
	private RandomAccessFile file;	//	file to be transfered.
	
	/**Constructor
	*@param DatagramSocket	soc		socket for host.
	*@param SocketAddress	rem		socket address of remote device.
	*@param String			fname	name of file to send.
	*@param int				fid 	hash of name of file to send.
	*/
	FileReceiver(DatagramSocket soc,SocketAddress rem,String fname,int fid)	throws FileNotFoundException{
		socket = soc;
		remote = rem;
		filename = fname;
		fileid = fid;
		
		packetsize = 8000;//Approx 8KB data in 1 packet.
		file = new RandomAccessFile(filename,"w");
	}
	
	/**dettaches the header [fileid ,index of packet data in original file].
	*
	*@return int	position of data in original file per packet size. -1 if fileid does not match. data is modified to have no header.
	*
	*@param  byte[]	data	data with header.
	*/
	public int dettachHeader(byte[] data){
		int index = -1;
		byte[] output = new byte[packetsize];
		try(ByteArrayInputStream bin = new ByteArrayInputStream(data)){
			try(DataInputStream in = new DataOutputStream(bin)){
				if(fileid == in.readInt()){
					index = in.readInt();
					in.read(output);
			}catch(IOException e){
				e.printStackTrace();
			}finally{}
			data = output;
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return index;
	}
	
	/**Receives packets
	*@return Thread thread which sends data as packets through socket.
	*/
	public Thread receivePackets(){
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
	
	/**recieves acknowlegment from reciever.
	*@return Thread thread recieve acknowlegment through socket.
	*/
	public Thread recieveAcknowledgement(){
		Thread t = new Thread(){
			public void run(){
				lock.readLock().lock();
				while(toSend.size()>0){
					lock.readLock().unlock();
					byte[] data = new byte[1001*Integer.BYTES];
					DatagramPacket dp = new DatagramPacket(data,data.length);
					try{
						socket.receive(dp);
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