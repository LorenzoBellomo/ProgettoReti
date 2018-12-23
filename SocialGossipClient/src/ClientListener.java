import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import condivise.Message;
import condivise.MessageHandler;
import condivise.RequestCode;
import condivise.RequestMessage;
import condivise.ResponseMessage;
import condivise.TextMessage;
import exceptions.MalformedMessageException;

/**
 * Classe del thread del client che ascolta sulla connessione dei messaggi e sul
 * socket multicast registrato sui gruppi delle varie chatroom a cui tale client
 * e' registrato. Un ciclo di ascolto e' fatto di un massimo di 200 ms di
 * ascolto sulla connessione dei messaggi, seguiti da uno scaricamento di quanti
 * piu' possibili datagram in arrivo dalle chatroom. Ricevuto un messaggio da un
 * utente o da una chatroom, l'utente aggiorna semplicemente la vista. Un altro
 * tipo di messaggio che puo' viaggiare sulla connessione dei messaggio e'
 * quello di richiesta di tipo: "OperP2PConnection". In tal caso il client apre
 * una connessione e comunica la risposta al server. Se una richiesta di
 * connessione P2P e' aperta, allora tale richiesta si inserisce nel ciclo di
 * ascolto del client, che fornisce una porzione del proprio tempo alla gestione
 * di tale connessione. A trasferimento di file terminato, il client chiude la
 * propria connessione temporanea e aggiorna la vista con un avviso
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class ClientListener extends Thread {

	// socket dei messaggi, questa classe non ha accesso a quello di controllo
	private Socket messageSocket;

	// il timeout del socket dei messaggi
	private int socketTimeout = 200;

	// l'handler della correlazione JSON <-> Message
	private MessageHandler messageHandler;

	// il socket multicast per la ricezione di messaggi dalle chatroom
	private MulticastSocket chatroomSocket;

	// la porta in caso di richiesta di un altro client per invio di file
	private int P2PPort;

	// server socket per le connessioni NIO con i peer
	private ServerSocketChannel serverChannel;

	private SG_Home gui;

	public ClientListener(Socket message, MulticastSocket chatroomSock, SG_Home home) {
		// COSTRUTTORE
		this.messageSocket = message;
		this.messageHandler = new MessageHandler();
		this.chatroomSocket = chatroomSock;
		gui = home;
	}

	public void run() {

		// inizializzo le variabili
		BufferedReader reader = null;
		try {
			// setto un timeout molto basso per il chatroom socket
			this.chatroomSocket.setSoTimeout(20);
			this.messageSocket.setSoTimeout(socketTimeout);
			reader = new BufferedReader(new InputStreamReader(messageSocket.getInputStream()));
			// apro il socketchannel per lo scambio di file
			this.serverChannel = ServerSocketChannel.open();
			// non voglio che si blocchi nelle accept
			this.serverChannel.configureBlocking(false);
		} catch (IOException e) {
			System.out.println("Errore con le connessioni");
			System.exit(1);
		}
		// devo ancora fare la bind
		boolean bound = false;
		// preparo il socket in cui accettero' le connessioni dei peer
		// richiedenti
		ServerSocket socket = serverChannel.socket();
		// voglio fare la bind su una porta a caso
		while (!bound) {
			// genero una porta
			this.P2PPort = (new Random()).nextInt(65535);
			// preparo l'indirizzo
			InetSocketAddress address = new InetSocketAddress(this.P2PPort);
			// provo a fare la bind
			try {
				socket.bind(address);
				// bind andata a buon fine
				bound = true;
			} catch (IOException e) {
				// fallita la bind, ignoro ed itero
			}
		}
		// sono riuscito a fare la bind, server socket e' pronto

		// ciclo while del listener
		while (!Thread.interrupted()) {
			// leggo prima la size (mi viene inviato un long)
			long messageSize = 0;

			try {
				String size = (reader.readLine());
				if (size == null) {
					// il server ha chiuso la connessione con me
					System.out.println("Server crashed?");
					System.exit(1);
				}
				// parso la stringa e ottengo il long
				messageSize = Long.parseLong(size);

				// adesso invece leggo il messaggio vero e proprio
				String msg = reader.readLine();

				if (msg == null) {
					// il server ha chiuso la connessione con me
					System.out.println("Server crashed?");
					System.exit(1);
				}

				if (messageSize != msg.length())
					System.out.println("Messaggio di lunghezza non corretta");
				// ignoro questo errore, provo comunque a vedere se il messaggio
				// e' sensato

				// parso il messaggio e ne ottengo il Message
				Message message = this.messageHandler.JSONString2Message(msg);

				// messaggio puo' essere una richiesta di apertura di
				// connessione o un messaggio di testo
				if (message.getType() == Message.REQUEST) {
					// richiesta, devo controllare che sia OPEN_P2P
					RequestMessage req = (RequestMessage) message;
					if (req.TypeOfRequest().equals(RequestCode.OPEN_P2PCONN)) {
						// devo aprire la connessione P2P
						ResponseMessage reply = ResponseMessage.BuildConnectionP2PAck(req.getReceiver(),
								Message.SERVERNAME, RequestCode.OPEN_P2PCONN, InetAddress.getByName("localhost"),
								this.P2PPort);
						messageHandler.sendMessage(messageSocket, reply);
						// ho inviato al server il mio indirizzo e la porta
					}
					// altrimenti non lo era e lo ignoro
				} else if (message.getType() == Message.TEXT) {
					// GUI UPDATE
					gui.newMessage(message.getSender(), ((TextMessage) message).getMessage());
				}
			} catch (SocketTimeoutException e) {
				// son stato fermo 200 ms ad aspettare una read
				// ignoro e vado al prossimo passo
			} catch (IOException e) {
				// ignore
			} catch (MalformedMessageException e) {
				// cosa mi e' stato inviato? ignoro
			}

			// controllato il socket dei messaggi, passo a controllare le
			// chatroom

			// il buffer dei datagram
			byte[] bytebuffer = new byte[2048];
			// per i datagrammi che ricevo
			DatagramPacket datagram = new DatagramPacket(bytebuffer, bytebuffer.length);

			// per sapere quando ho finito di ottenere nuovi datagram
			boolean timeout = false;
			try {
				// ricevo fino a che non mi fermo per un timeout messaggi
				// dalle chatroom
				while (!timeout) {
					this.chatroomSocket.receive(datagram);
					// aggiorno la gui in modo che il messaggio venga
					// visualizzato
					System.out.println("prima " + new String(datagram.getData()));
					String msg = new String(datagram.getData()).trim();
					System.out.println("RICEVUTO GRUPPO "+msg);
					// ottengo il messaggio ricevuto
					TextMessage message = (TextMessage) messageHandler.JSONString2Message(msg);
					// ricevuto un messaggio, aggiorno la gui
					gui.newMessage(message.getReceiver(),  "("+message.getSender()+") "+message.getMessage());

				}
			} catch (SocketTimeoutException e) {
				// ricevuto timeout, fermo il ciclo
				timeout = true;
			} catch (IOException e) {
				// e' stata chiusa la connessione dei messaggi con il server
				System.out.println("Server crashed?");
				System.exit(1);
			} catch (MalformedMessageException e) {
				// errore nel messaggio, ignoro
			}
			/* adesso devo fare la gestione dei file */
			SocketChannel fileSocket;
			try {
				// provo a fiducia una accept, se ritorna null nessuno
				// vuole mandarmi file
				fileSocket = this.serverChannel.accept();
				if (fileSocket != null) {
					// ho accettato qualcuno
					// gestisco l'invio file
					this.handleFileTransfer(fileSocket);
					fileSocket.close();
				}
			} catch (IOException e) {
				// errore, ignoro
			}
		}
	}

	/**
	 * Legge il nome del file e il file che il peer mi vuole inviare, copiandone
	 * il contenuto in un file con lo stesso nome dell'originale che viene
	 * creato appositamente nella cartella corrente
	 * 
	 * @param fileSocket
	 *            il socketchannel da cui leggere il file
	 * @return il nome del file creato
	 * @throws IOException
	 *             se succede un errore di IO nella lettura
	 */
	private String handleFileTransfer(SocketChannel fileSocket) throws IOException {

		// buffer per la size (un long) del file
		ByteBuffer sizeBuf = ByteBuffer.allocateDirect(Long.BYTES);

		// la size (prima del nome del file, poi del file
		long size = 0;
		// il nome del file
		String fileName = null;
		int bytesRead = 0;

		// leggo per prima cosa la size (un long)
		while (fileSocket.read(sizeBuf) != -1) {
			if (!sizeBuf.hasRemaining()) {
				// finito di leggere un long
				sizeBuf.flip();
				size = sizeBuf.getLong();
				break;
			}
		}
		if (size == 0)
			throw new IOException();

		// letta la size, mi alloco un buffer di quella dimensione per leggere
		// il fileName
		ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);

		// leggo adesso il nome del file
		while ((bytesRead += fileSocket.read(buffer)) != -1) {
			if (bytesRead == size) {
				// letto il nome del file intero
				buffer.flip();
				byte[] strBytes = new byte[buffer.limit()];
				buffer.get(strBytes);
				fileName = new String(strBytes);
				break;
			}
		}

		// leggo adesso la size del file
		buffer = ByteBuffer.allocateDirect(1024);
		sizeBuf.clear();
		while (fileSocket.read(sizeBuf) != -1) {
			if (!sizeBuf.hasRemaining()) {
				sizeBuf.flip();
				size = sizeBuf.getLong();
				break;
			}
		}

		if (size == 0)
			throw new IOException();

		buffer.clear();

		FileChannel outChannel = FileChannel.open(Paths.get(fileName).getFileName(),
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		// creo un file nella directory corrente con lo stesso nome
		// del file richiesto e vi inserisco il contenuto del file richiesto
		while (fileSocket.read(buffer) != -1) {
			// leggo tutto il file dal server e lo scrivo tutto sul
			// nuovo file
			buffer.flip();
			while (buffer.hasRemaining())
				outChannel.write(buffer);
			buffer.clear();
		}

		// informo l'utente che il file e' stato ricevuto correttamente
		gui.displayDialogWindow(11);

		// restituisco il nome del file creato
		return Paths.get(fileName).getFileName().toString();
	}

}
