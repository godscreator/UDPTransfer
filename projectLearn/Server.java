import static java.lang.Thread.currentThread;

import java.util.concurrent.TimeUnit;

/**
 * Java Program to demonstrate how to stop a thread in Java.
 * There is a stop() method in Thread class but its deprecated 
 * because of deadlock and other issue, but its easy to write
 * your own stop() method to stop a thread in Java. 
 * 
 * @author java67
 */

class Server implements Runnable{
    private volatile boolean exit = true;
    
	@Override
    public void run() {
        while(this.exit){
            System.out.println("Server is running.....");
        }
        
        System.out.println("Server is stopped....");
    }
    
    public void stop(){
        this.exit = false;
    }
	
	public static void main(String args[]) throws InterruptedException {
        Server myServer = new Server();

        Thread t1 = new Thread(myServer, "T1");
        t1.start();
        
        //Now, let's stop our Server thread
        System.out.println(currentThread().getName() + " is stopping Server thread");
        myServer.stop();
        
        //Let's wait to see server thread stopped 
        //TimeUnit.MILLISECONDS.sleep(200);
        t1.join();
        System.out.println(currentThread().getName() + " is finished now");
    }
}
