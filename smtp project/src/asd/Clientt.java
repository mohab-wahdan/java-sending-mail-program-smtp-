package asd;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class Clientt {

	public static void main(String[] args) {
		InetAddress ip;
		try {
			ip = InetAddress.getByName("localhost");
			Socket clientSocket = new Socket(ip, 3000);

			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			Scanner scanner = new Scanner(System.in);
			String ask = "";
			while (true) {
				ask = input.readUTF();
				System.out.println("Server: " + ask);

				String email = scanner.nextLine();
				output.writeUTF(email);

				ask = input.readUTF();
				System.out.println("Server: " + ask);

				String password = scanner.nextLine();
				output.writeUTF(password);

				int err = input.read();
				if (err == 250) {
					System.out.println("Server: connected successfully");
					break;
				} else {
					System.out.println("Server: invalid username or password");
				}
			}

			while (true) {
				String readOptions = input.readUTF();
				System.out.println("Server: " + readOptions);

				String option = scanner.nextLine();
				option = option.toLowerCase();
				if (option.contains("show mail") && option.matches(".*\\d.*")) {
					output.writeUTF("show mail");
					String emailId = option.replaceAll("\\D+", "");
					output.writeUTF(emailId);
					String validId = input.readUTF();
					if (validId.contentEquals("valid")) {
						String mail = input.readUTF();
						System.out.println(mail);
						continue;
					}
					System.out.println("mail doesn't exist");
					continue;
				}
				if (option.contains("show sent mail") && option.matches(".*\\d.*")) {
					output.writeUTF("show sent mail");
					String emailId = option.replaceAll("\\D+", "");
					output.writeUTF(emailId);
					String validId = input.readUTF();
					if (validId.contentEquals("valid")) {
						String mail = input.readUTF();
						System.out.println(mail);
						continue;
					}
					System.out.println("mail doesn't exist");
					continue;
				}
				option = option.toUpperCase();
				output.writeUTF(option);
				int exit = 0;
				option = option.toLowerCase();
				switch (option) {
				case "send new":
					int replyCode;
					while (true) {
						ask = input.readUTF();
						System.out.println("Server: " + ask);
						ask = input.readUTF();
						System.out.println("Server: " + ask);

						String rcpt = scanner.nextLine();
						output.writeUTF(rcpt);

						replyCode = input.read();
						if (replyCode == 250) {
							System.out.println("Server confirms recipient");
							break;
						} else {
							System.out.println("this email doesn't exist");
							continue;
						}
					}
					ask = input.readUTF();
					System.out.println("Server: " + ask);
					String subject = scanner.nextLine();
					output.writeUTF(subject);

					replyCode = input.read();
					if (replyCode == 250)
						System.out.println("Server confirms subject");

					ask = input.readUTF();
					System.out.println("Server: " + ask);

					String data = "";
					String line = "";
					while (true) {
						line = scanner.nextLine();
						data += line;
						if (line.equalsIgnoreCase("."))
							break;
					}
					output.writeUTF(data);
					replyCode = input.read();
					if (replyCode == 250)
						System.out.println("Server accepted the message for delivery");

					ask = input.readUTF();
					System.out.println("Server: " + ask);

					subject = scanner.nextLine();
					output.writeUTF(subject);
					replyCode = input.read();
					if (replyCode == 250) {
						System.out.println("Please select the file");
						File file = new ChooseFile().getFile();
						String ext1 = getExtensionByStringHandling(file.getAbsolutePath());

						String bytes = encodeFileToBase64Binary(file);
						output.writeUTF(bytes);
						output.writeUTF(ext1);
						replyCode = input.read();
						if (replyCode == 250) 
							System.out.println("Server confirms attachment");
					}
					break;
				case "show mailbox":
					System.out.println("client: show mailbox");
					String validRange = input.readUTF();
					if (validRange.contentEquals("invalid")) {
						System.out.println("there is no mails to show");
						break;
					} else {
						String mbox = input.readUTF();
						System.out.println(mbox);
					}
					break;
				case "show sent":
					System.out.println("client: show sent");
					String SvalidRange = input.readUTF();
					if (SvalidRange.contentEquals("invalid")) {
						System.out.println("there is no mails to show");
						break;
					} else {
						String mbox = input.readUTF();
						System.out.println(mbox);
					}
					break;
				case "quit":
					exit = 1;
					break;
				}
				if (exit == 1)
					break;
			}
			scanner.close();
			System.out.println("connection closed");
			clientSocket.close();
		} catch (UnknownHostException e) {
			System.out.println("Unknown host");
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Problem in I/O of Client Socket");
		}

	}

	private static String encodeFileToBase64Binary(File file) {
		String encodedfile = null;
		try {
			FileInputStream fileInputStreamReader = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];
			fileInputStreamReader.read(bytes);
			encodedfile = Base64.getEncoder().encodeToString(bytes);
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
		}
		return encodedfile;
	}

	public static String getExtensionByStringHandling(String filename) {
		if (filename.contains("."))
			return filename.substring(filename.indexOf("."), filename.length());
		else
			return "";
	}

	public static class ChooseFile {
		private JFrame frame;

		public ChooseFile() {
			frame = new JFrame();

			frame.setVisible(true);
			BringToFront();
		}

		public File getFile() {
			JFileChooser fc = new JFileChooser();
			if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(null)) {
				frame.setVisible(false);
				return fc.getSelectedFile();
			} else {
				System.out.println("Next time select a file.");
				System.exit(1);
			}
			return null;
		}

		private void BringToFront() {
			frame.setExtendedState(JFrame.ICONIFIED);
			frame.setExtendedState(JFrame.NORMAL);

		}

	}
}