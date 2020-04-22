package asd;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.util.Scanner;

import javax.lang.model.util.ElementScanner6;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class Client {
	public String Email, Password;
	public int ClientId;
	public static int ClientCount = 1;
	List<String> emailOverview = new ArrayList<>();
	List<String> sentEmailOverview = new ArrayList<>();

	public String showEmails(String emailId, int type) {
		String mFolder = "Inbox";
		if (type == 1)
			mFolder = "Sent";
		File file = new File(System.getProperty("user.dir") + "/MailBox/Client" + ClientId + "/" + mFolder + "/Mail"
				+ emailId + "/mail" + emailId + ".txt");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}

		String st, whole = "";
		try {
			while ((st = br.readLine()) != null)
				whole = whole + st + "\n";

			// System.out.println(st);
		} catch (IOException e) {
			System.out.println(e);
		}
		return whole;
	}

	public Client(String email, String password) {
		Email = email;
		Password = password;
		ClientId = ClientCount;
		ClientCount++;
	}
}

public class Server {

	static List<Client> clients = new ArrayList<Client>();
	static int connectedClient;
	static int rcptClient;

	public static void main(String[] args) {

		try {
			ServerSocket serverSocket = new ServerSocket(3000);
			LoadUsers();
			System.out.println("Server is now booted up and is waiting for any client to connect");
			while (true) {
				Socket clientSocket = serverSocket.accept();
				Thread client = new ClientConnection(clientSocket);
				client.start();
			}
		} catch (IOException e) {
			System.out.println("Problem with I/O Server Socket" + e);
		}

	}

	public static void LoadUsers() {
		File file = new File(System.getProperty("user.dir") + "/emails.txt");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}

		String st;
		String ml = "";
		String pass = "";
		int i = 0;
		try {
			while ((st = br.readLine()) != null) {
				if (i == 0) {
					ml = st;
					i++;
				} else {
					pass = st;
					clients.add(new Client(ml, pass));
					i = 0;
				}
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public static boolean verifyClient(String email, String password) {
		for (int i = 0; i < clients.size(); i++) {
			if (email.equalsIgnoreCase(clients.get(i).Email)) {
				if (password != "") {
					if (password.equalsIgnoreCase(clients.get(i).Password)) {
						connectedClient = i;
						return true;
					}
				} else {
					rcptClient = i;
					return true;
				}
			}
		}
		return false;
	}

	private static void decodeBase64BinaryToFile(String bytes, String path) {
		byte[] decodedfile = null;
		try {
			decodedfile = Base64.getDecoder().decode(bytes);
			FileOutputStream fos = new FileOutputStream(path);
			fos.write(decodedfile);
		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		}
	}

	static class ClientConnection extends Thread {
		final private Socket clientSocket;

		public ClientConnection(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		public void run() {
			DataInputStream input;
			try {
				input = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

				String clientRequest = "";
				String rcpt = "";
				String subject = "";
				String mailBody = "";
				String bytes = "";
				String extension = "";
				String attach = "";
				int ext = 0;

				while (true) {
					output.writeUTF("Email:");
					String email = input.readUTF();

					output.writeUTF("Password:");
					String password = input.readUTF();

					if (verifyClient(email, password)) {
						System.out.println("Client: " + email + " is connected");
						output.write(250);
						break;
					} else {
						output.write(404);
					}
				}
				while (true) {
					output.writeUTF(
							"OPTIONS: SEND NEW, SHOW MAILBOX, SHOW MAIL mail_id, SHOW SENT, SHOW SENT MAIL mail_id, QUIT");
					clientRequest = input.readUTF();
					switch (clientRequest.toLowerCase()) {
					case "send new":
						while (true) {
							output.writeUTF("MAIL FROM: " + clients.get(connectedClient).Email);
							output.writeUTF("RCPT TO:");
							rcpt = input.readUTF();

							if (verifyClient(rcpt, "")) {
								output.write(250);
								break;
							} else {
								output.write(404);
								continue;
							}
						}
						output.writeUTF("SUBJECT:");
						subject = input.readUTF();
						output.write(250);

						output.writeUTF("DATA:");
						mailBody = input.readUTF();
						output.write(250);

						output.writeUTF("Send A to Attach A file:");
						attach = input.readUTF();
						if (attach.equalsIgnoreCase("a")) {
							output.write(250);
							bytes = input.readUTF();
							extension = input.readUTF();
							output.write(250);
						} else
							output.write(404);

						clients.get(rcptClient).emailOverview
								.add("From:" + clients.get(connectedClient).Email + "\n" + "Subject: " + subject);
						clients.get(connectedClient).sentEmailOverview
								.add("To:" + clients.get(rcptClient).Email + "\n" + "Subject: " + subject);
						int mailCount = clients.get(rcptClient).emailOverview.size();
						int sentMailCount = clients.get(connectedClient).sentEmailOverview.size();
						SaveMail(clients.get(connectedClient).Email, rcpt, subject, mailBody, rcptClient + 1,
								connectedClient + 1, mailCount, sentMailCount);

						if (attach.equalsIgnoreCase("a")) {
							decodeBase64BinaryToFile(bytes, System.getProperty("user.dir") + "/MailBox/Client"
									+ (connectedClient + 1) + "/Sent/Mail" + mailCount + "/Attachment" + extension);
							decodeBase64BinaryToFile(bytes, System.getProperty("user.dir") + "/MailBox/Client"
									+ (rcptClient + 1) + "/Inbox/Mail" + mailCount + "/Attachment" + extension);
						}
						break;
					case "show mailbox":
						if (clients.get(connectedClient).emailOverview.size() < 1) {
							output.writeUTF("invalid");
							break;
						} else {
							output.writeUTF("valid");
						}

						int c = clients.get(connectedClient).emailOverview.size();
						String mailBox = "";
						for (int i = 0; i < c; i++) {
							mailBox += "mail: " + (i + 1) + "\n" + clients.get(connectedClient).emailOverview.get(i)
									+ "\n";
						}
						output.writeUTF(mailBox);
						break;
					case "show sent":
						if (clients.get(connectedClient).sentEmailOverview.size() < 1) {
							output.writeUTF("invalid");
							break;
						} else {
							output.writeUTF("valid");
						}

						int v = clients.get(connectedClient).sentEmailOverview.size();
						String sent = "";
						for (int i = 0; i < v; i++) {
							sent += "mail: " + (i + 1) + "\n" + clients.get(connectedClient).sentEmailOverview.get(i)
									+ "\n";
						}
						output.writeUTF(sent);
						break;
					case "show mail":
						String mailId = input.readUTF();
						int id = Integer.valueOf(mailId);
						if (id > clients.get(connectedClient).emailOverview.size()
								|| clients.get(connectedClient).emailOverview.size() == 0) {
							output.writeUTF("invalid");
							break;
						} else
							output.writeUTF("valid");
						output.writeUTF(clients.get(connectedClient).showEmails(mailId, 0));
						break;
					case "show sent mail":
						String sMailId = input.readUTF();
						int Sid = Integer.valueOf(sMailId);
						if (Sid > clients.get(connectedClient).sentEmailOverview.size()
								|| clients.get(connectedClient).sentEmailOverview.size() == 0) {
							output.writeUTF("invalid");
							break;
						} else
							output.writeUTF("valid");
						output.writeUTF(clients.get(connectedClient).showEmails(sMailId, 1));
						break;
					case "quit":
						ext = 1;
						break;
					}
					if (ext == 1) {
						clientSocket.close();
						break;
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			}

		}

		public void SaveMail(String sender, String rcpt, String subject, String mailBody, int rcptId, int connectedId,
				int mailCount, int sentMailCount) {
			sender = "MAIL FROM:" + sender;
			rcpt = "RCPT TO:" + rcpt;
			subject = "SUBJECT:" + subject;
			mailBody = "DATA:" + mailBody;
			try {
				File file = new File(System.getProperty("user.dir") + "/MailBox");
				file.mkdir();
				file = new File(System.getProperty("user.dir") + "/MailBox/Client" + rcptId);
				file.mkdir();
				file = new File(System.getProperty("user.dir") + "/MailBox/Client" + rcptId + "/Inbox");
				file.mkdir();
				file = new File(
						System.getProperty("user.dir") + "/MailBox/Client" + rcptId + "/Inbox/Mail" + mailCount);
				file.mkdir();
				file = new File(System.getProperty("user.dir") + "/MailBox/Client" + connectedId);
				file.mkdir();
				file = new File(System.getProperty("user.dir") + "/MailBox/Client" + connectedId + "/Sent");
				file.mkdir();
				file = new File(
						System.getProperty("user.dir") + "/MailBox/Client" + connectedId + "/Sent/Mail" + sentMailCount);
				file.mkdir();
				BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir")
						+ "/MailBox/Client" + rcptId + "/Inbox/Mail" + mailCount + "/mail" + mailCount + ".txt"));
				writer.write(sender);
				writer.newLine();
				writer.write(rcpt);
				writer.newLine();
				writer.write(subject);
				writer.newLine();
				writer.write(mailBody);
				writer.newLine();
				writer.close();
				writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/MailBox/Client"
						+ connectedId + "/Sent/Mail" + sentMailCount + "/mail" + sentMailCount + ".txt"));
				writer.write(sender);
				writer.newLine();
				writer.write(rcpt);
				writer.newLine();
				writer.write(subject);
				writer.newLine();
				writer.write(mailBody);
				writer.newLine();
				writer.close();
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}