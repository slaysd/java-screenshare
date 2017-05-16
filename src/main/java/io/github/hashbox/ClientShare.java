package io.github.hashbox;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


public class ClientShare implements Runnable{
	ClientGUI clntGUI;
	boolean select_flag;
	boolean isSharing = true;
	String ip_address;
	final int recv_port = 9001;
	final int send_port = 9002;
	final double DATAGRAM_MAX_SIZE = 65527;
	
	
	public ClientShare(ClientGUI clntGUI, boolean select_flag, String ip_address) {
		this.clntGUI = clntGUI;
		this.select_flag = select_flag;
		this.ip_address = ip_address;
	}

	public void run() {
		System.out.println("[clientShare.java]client Share Thread START");
		// TODO Auto-generated method stub
		Rectangle rect;
		Robot rb = null;
		rect = new Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
		BufferedImage temp = null;
		DatagramSocket send_ds = null;
		DatagramSocket recv_ds = null;
		DatagramPacket recv_dp = null;
		ImageIcon icon_screen = new ImageIcon();
		byte[] buf = new byte[(int)DATAGRAM_MAX_SIZE];
		
		try {
			send_ds = new DatagramSocket();
			recv_ds = new DatagramSocket(recv_port);
			recv_dp = new DatagramPacket(buf, buf.length);
			rb = new Robot();
		} catch (AWTException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(isSharing) {
			if(select_flag) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				System.out.println("[clientShare.java]Capturing Screen now.");
				temp = rb.createScreenCapture(rect);
				temp = scaleImage(temp, 640, 480);
				try {
					ImageIO.write(temp, "jpg", baos);
					baos.flush();
					byte[] imageInByte = baos.toByteArray();
					buf = imageInByte;
					System.out.println("[clientShare.java]Screen byte size : " + baos.size());
					baos.close();
					compressBytes(buf);
					//DatagramPacket send_dp = new DatagramPacket(imageInByte, imageInByte.length, InetAddress.getByName(ip_address), send_port);
					DatagramPacket send_dp = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip_address), send_port);
					send_ds.send(send_dp);
					System.out.println("[clientShare.java]Send Screen Sharing Data.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				recv_ds.receive(recv_dp);
				System.out.println("[clientShare.java]Recv Broadcast Screen Sharing Data.");
				byte[] imageInByte = new byte[(int) DATAGRAM_MAX_SIZE]; 
				imageInByte = recv_dp.getData();
				System.out.println("[clientShare.java]Screen byte size recv : " + recv_dp.getLength());
				//imageInByte = extractBytes(imageInByte);
				InputStream in = new ByteArrayInputStream(imageInByte);
				BufferedImage screen = ImageIO.read(in);
				if(screen==null) {
					continue;
				}
				icon_screen.setImage(screen);
				clntGUI.lb_screen.setIcon(icon_screen);
				clntGUI.lb_screen.setText(null);
				clntGUI.lb_screen.repaint();
				System.out.println("[clientShare.java]Update Screen Sharing Data.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		clntGUI.lb_screen.setIcon(null);
		clntGUI.lb_screen.setText("화면 공유 없음");
		clntGUI.lb_screen.repaint();
		recv_ds.close();
		send_ds.close();
		System.out.println("[clientShare.java]Stop clientShare Thread.");
	}
	
	public byte[] compressBytes(byte[] data) throws UnsupportedEncodingException, IOException
    {
        byte[] input = data;  //the format... data is the total string
        Deflater df = new Deflater();       //this function mainly generate the byte code
        
        df.setLevel(9);
        df.setInput(input);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);   //we write the generated byte code in this array
        df.finish();
        byte[] buff = new byte[(int) DATAGRAM_MAX_SIZE];   //segment segment pop....segment set 1024
        while(!df.finished())
        {
            int count = df.deflate(buff);       //returns the generated code... index
            baos.write(buff, 0, count);     //write 4m 0 to count
        }
        baos.close();
        byte[] output = baos.toByteArray();
 
        System.out.println("Original: "+input.length);
        System.out.println("Compressed: "+output.length);
        return output;
    }
     
    public byte[] extractBytes(byte[] input) throws UnsupportedEncodingException, IOException, DataFormatException
    {
        Inflater ifl = new Inflater();   //mainly generate the extraction
        //df.setLevel(Deflater.BEST_COMPRESSION);
        ifl.setInput(input);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
        byte[] buff = new byte[(int) DATAGRAM_MAX_SIZE];
        while(!ifl.finished())
        {
            int count = ifl.inflate(buff);
            baos.write(buff, 0, count);
        }
        baos.close();
        byte[] output = baos.toByteArray();
 
        System.out.println("Original: "+input.length);
        System.out.println("Extracted: "+output.length);
        //System.out.println("Data:");
        //System.out.println(new String(output));
        return output;
    }
    public BufferedImage scaleImage(BufferedImage img, int width, int height) {
	    int imgWidth = img.getWidth();
	    int imgHeight = img.getHeight();
	    if (imgWidth * height < imgHeight * width) {
	        width = imgWidth * height / imgHeight;
	    } else {
	        height = imgHeight * width / imgWidth;
	    }
	    BufferedImage newImage = new BufferedImage(width, height,
	            BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = newImage.createGraphics();
	    try {
	        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	        g.clearRect(0, 0, width, height);
	        g.drawImage(img, 0, 0, width, height, null);
	    } finally {
	        g.dispose();
	    }
	    return newImage;
	}
}