package projectLearn;
import java.io.*;

public class FileTransferSystem
{
	public static void readFile(InputStream source,String filename) throws IOException
	{
		FileOutputStream destination = new FileOutputStream(filename);
		copy(source,destination);
		destination.close();
	}
	public static void writeFile(String filename,OutputStream destination) throws IOException
	{
		FileInputStream source = new FileInputStream(filename);
		copy(source,destination);
		source.close();
	}
	public static void copy(InputStream source,OutputStream destination) throws IOException
	{
		int i;
		try
		{
			//System.out.println("Here");
			do{
				//System.out.println("Here1");
				i = source.read();
				if(i!=-1){destination.write(i);}//System.out.print((char)i);}
			}while(i!=-1);
		}catch(IOException e){
			System.out.println("File error");
		}
	}
}