// GET /?function=login&username=ben&password=sponge HTTP/1.1
// make sure to send an empty line after

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TestClient extends Thread {
	private BufferedReader br;
	private PrintWriter pw;
	
	public TestClient(String hostname, int port)
	{
		Socket s = null;
		try {
			System.out.println("Trying to connect to " + hostname +":"+ port);
			s = new Socket (hostname, port);
			System.out.println("Connected.");
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw = new PrintWriter(s.getOutputStream());
			this.start();
			Scanner scan = new Scanner(System.in);
			while(true) {
				String line = scan.nextLine();
				pw.println(line);
				pw.flush();
			}
		} catch(IOException ioe) {
			System.out.println("ioe: "+ioe.getMessage());
		} finally {
			try {
				if(s!=null) s.close();
				if(br!=null) br.close();
				if(pw!=null) pw.close();
			} catch(IOException ioe) {
				System.out.println("bluhhh");
			}
		}
	}
	public void run()
	{
		try {
			while(true) {
				String line = br.readLine();
				System.out.println(line);
			}
		} catch (IOException ioe) {
			System.out.println("ioe in run: " + ioe.getMessage());
		}
	}
	public static void main(String[] args)
	{
		new TestClient("localhost", 8080);
	}
}
