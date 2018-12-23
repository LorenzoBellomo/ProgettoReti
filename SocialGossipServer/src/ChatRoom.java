import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import exceptions.AlreadyInChatroomException;

/**
 * Classe che modella le chatroom, che sono caratterizzate da un
 * identificativo, ovvero una stringa che le rappresenta, un indirizzo
 * multicast ed un elenco di utenti che vi sono registrati. Questa classe
 * inoltre tiene traccia del prossimo indirizzo multicast da assegnare ad
 * una chatroom nuova
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class ChatRoom {
	
	// la porta per tutte le chatroom
	public static final int MULTICAST_PORT = 7080;

	// il nome della chatroom
	private String chatName;

	// l'indirizzo multicast della chatroom
	private InetAddress multicastAddress;

	// variabile statica, indica l'indirizzo multicast della prossima chat
	// che verra' creata
	private static String nextFreeIP = "225.0.0.0";

	// l'elenco degli utenti di questa chatroom
	private ArrayList<String> users;

	public ChatRoom(String chatName, String creator) {
		// COSTRUTTORE
		this.chatName = chatName;
		try {
			// parso il prossimo indirizzo buono e lo metto come indirizzo di
			// questa chatroom
			this.multicastAddress = InetAddress.getByName(nextFreeIP);
		} catch (UnknownHostException e) {
			System.out.println("Ip Errato: " + nextFreeIP);
		}
		// creo l'array di utenti presenti e aggiungo il creator
		this.users = new ArrayList<>();
		this.users.add(creator);

		// aggiorno il next Free IP
		// divido la stringa del nuovo IP sul punto e ottengo i
		// numeri singoli da 0 a 255
		String[] octet = nextFreeIP.split("\\.");
		// aumento di uno l'indirizzo
		for (int i = 3; i > 0; i--) {
			// parto dall'ottetto sulla destra, e controllo quanto vale
			int toUpdate = Integer.parseInt(octet[i]);
			if (toUpdate != 255) {
				// se non vale 255 ho finito, aggiungo uno e ho fatto
				toUpdate++;
				octet[i] = toUpdate + "";
				break;
			} else // vale 255, devo metterlo a 0 e passare all'ottetto dopo
				octet[i] = "0";
		}
		// concateno di nuovo gli ottetti e ottengo un nuovo IP
		nextFreeIP = octet[0] + "." + octet[1] + "." + octet[2] + "." + octet[3];
	}

	/**
	 * Metodo getter, ottiene il nome della chatroom
	 * 
	 * @return il nome della chatroom
	 */
	public String getChatName() {
		return this.chatName;
	}

	/**
	 * Metodo getter, ottiene l'indirizzo della chatroom
	 * 
	 * @return L'indirizzo della chatroom
	 */
	public InetAddress getAddress() {
		return this.multicastAddress;
	}

	/**
	 * Metodo getter, restituisce l'elenco degli utenti di tale chatroom
	 * 
	 * @return l'elenco degli utenti registrati a tale chatroom
	 */
	public String[] getUserList() {
		return this.users.toArray(new String[0]);
	}

	/**
	 * Aggiunge un utente alla chatroom se esso non vi fosse presente. Se invece
	 * vi fosse gia' presente non fa nulla
	 * 
	 * @param nickname
	 *            il nickname dell'utente da aggiungere alla chatRoom
	 * @throws AlreadyInChatroomException
	 *             se l'utente e' gia' nella chatroom richiesta
	 */
	public void addUser(String nickname) throws AlreadyInChatroomException {
		if (nickname == null)
			throw new NullPointerException();

		if (!this.users.contains(nickname))
			this.users.add(nickname);
		else
			throw new AlreadyInChatroomException();
	}
	
	
	/**
	 * restituisce il valore di verita' di: user e' nella chatroom
	 * 
	 * @param user l'utente di cui voglio verificare l'appartenenza
	 * @return true se l'utente e' nella chatroom, false altrimenti
	 */
	public boolean isInChatroom(String user) {
		if(user == null) throw new NullPointerException();
		
		return this.users.contains(user);
	}
}
