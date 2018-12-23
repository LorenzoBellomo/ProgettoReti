import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;

/**
 * Classe che si occupa della gestione dell'interfaccia grafica del client,
 * implementando l'homepage di Social Gossip, a cui un utente accede loggandosi
 * in nella GUI implementata nella classe AccessWindow, e da cui l'utente puo'
 * accedere ad ogni funzionalita' offerta dal servizio (Chattare con
 * amici/gruppi, aggiunta di amici e cosi via).
 * 
 * Questa classe contiene anche le funzioni necessarie per l'aggiornamento della
 * GUI a seguito di eventi ricevuti da altri client o dal server (ricezione di
 * un file, di un messaggio ecc..)
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class SG_Home extends JFrame {

	private static final long serialVersionUID = 1L;

	// pannello contente gli amici del client loggato
	private JPanel friendslist;

	// pannello contente le chat attive del client
	private JPanel chats;

	// numero di amici del client
	private static int friendscounter = 0;

	// numero di chatroom a cui il client appartiene
	private static int chatroomscounter = 0;

	// contiene tutte le chat attive
	private JTextArea chat_area[] = new JTextArea[64];

	// stato possibile di un client
	public enum status {
		online, offline
	}

	// contiene gli amici del client e il loro status (offline/online)
	// verra' modificata a seguito di una invocazione della callback
	// RMI fornita al server
	private HashMap<String, status> myFriends;

	// contiene le chatroom del client
	private ArrayList<String> myChatRooms;

	private int activeChatArea = 0;

	// istanza della classe ClientOps utilizzata per
	// effettuare richieste al server
	private ClientOps reqHandler;

	/**
	 * Il costruttore di questa classe si occupa del setup dell'intera GUI,
	 * settando i parametri e i valori della home, creando i vari componenti
	 * grafici e mostrandoli in un layout non predefinito da JAVA
	 */
	public SG_Home() {
		myFriends = new HashMap<String, status>();
		myChatRooms = new ArrayList<String>();

		JPanel panel = (JPanel) getContentPane();
		panel.setLayout(null);
		setSize(new Dimension(1024, 728));
		setLocation(300, 100);

		// creo i controlli della mia interfaccia grafica
		// barra di ricerca di un utente
		JTextField searchBar = new JTextField();
		searchBar.setBounds(50, 10, 170, 30);
		searchBar.setText("Cerca utente..");
		// bottone per la ricerca
		JButton searchBtn = new JButton("Cerca");
		searchBtn.setBounds(240, 10, 80, 30);

		searchBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// faccio la richiesta di lookup di un utente
				String username = searchBar.getText();
				int result = reqHandler.NewLookup(username);
				// mostro il messaggio corrispondente al risultato
				// dell'operazione
				displayDialogWindow(result);
			}

		});

		// lista degli amici online
		friendslist = new JPanel();
		// la friendslist occupera' l'intera parte destra dell'interfaccia
		friendslist.setBounds(getSize().width - 200, 5, 180, getSize().height);
		;
		friendslist.setLayout(null);
		// aggiungo una scrollbar alla lista di amici
		// JScrollBar scrollbar = new JScrollBar();
		// friendslist.add(scrollbar);
		friendslist.setVisible(true);
		friendslist.setBorder(BorderFactory.createTitledBorder("Amici"));

		// pannello delle chat in basso
		chats = new JPanel();
		chats.setBounds(0, getSize().height - 80, getSize().width - 200, 40);
		chats.setBackground(new Color(0, 0, 255)); // blue bar
		chats.setLayout(new FlowLayout());
		chats.setVisible(true);
		// label delle chat
		JLabel chat_label = new JLabel("Chat attive");
		chat_label.setBounds(5, getSize().height - 100, 80, 25);

		// chat field
		JTextArea chat_input_field = new JTextArea();
		chat_input_field.setBorder(BorderFactory.createTitledBorder("Chat"));
		chat_input_field.setLineWrap(true);

		// send button
		JButton send = new JButton("Invia");
		// aggiungo funzionalita' al bottone
		send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// invio il messaggio al server
				String messageText = chat_input_field.getText();
				chat_input_field.setText("");
				// prendo la chat attualmente attiva, da cui estraggo il
				// destinatario
				JTextArea a = chat_area[activeChatArea];
				int result;

				// controllo se il msg e' destinato ad una chatroom
				if (myChatRooms.contains(a.getName())) {
					result = reqHandler.MessageToChatRoom(a.getName(), messageText);
					// mostro solo messaggi di errore, quello testuale arrivera'
					// con udp
				} else { // il destinatario e' un utente
					result = reqHandler.Message2Friend(a.getName(), messageText);
					if (result == 0) {
						// tutto ok, mostro il messaggio nella chat area
						a.setText(a.getText() + "\n" + "[Io]: " + messageText);
						// non mostro messaggi di conferma in questo caso
					}
				}
				if (result != 0)
					displayDialogWindow(result);

				// a.setCaretPosition(a.getText().length() - 1);

			}
		});

		// set up
		chat_input_field.setBounds(250, getSize().height - 170, 350, 80);
		send.setBounds(620, getSize().height - 170, 80, 40);

		// send file button
		JButton sendFile = new JButton("Invia File");
		// aggiungo funzionalita' al bottone
		sendFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// invio il messaggio di richiesta al server
				String filename = chat_input_field.getText();
				chat_input_field.setText("");
				// prendo la chat attualmente attiva, da cui estraggo il
				// destinatario
				JTextArea a = chat_area[activeChatArea];

				int result = reqHandler.File2Friend(a.getName(), filename);

				if (result == 0) {
					// tutto ok
					a.setText(a.getText() + "\n" + "[Io]FILE: " + filename);
					displayDialogWindow(result);
				} else {
					// c'e' stato un errore nell'invio
					displayDialogWindow(result);
				}
				// a.setCaretPosition(a.getText().length() - 1);

			}
		});

		sendFile.setBounds(710, getSize().height - 170, 100, 40);

		// Interfaccia per la gestione delle operazioni definite in 'options'
		String options[] = { "Aggiungi Amico", "Crea Chat Room", "Elimina Chat Room", "Join Chat Room" };
		JComboBox<String> combo = new JComboBox<>(options);
		combo.setBounds(350, 10, 160, 30);

		JTextField multipleActions = new JTextField();
		multipleActions.setBounds(520, 10, 150, 30);

		JButton performAction = new JButton("Esegui");
		performAction.setBounds(675, 10, 90, 30);
		performAction.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				// eseguiamo l'operazione corrispondente in
				// base a quella scelta nella ComboBox
				int index = combo.getSelectedIndex();
				if (index == -1)
					return;
				int result;

				// prendo l'idchat/username
				String target = multipleActions.getText();
				multipleActions.setText("");

				switch (index) {
				case 0: { // Aggiungi un amico
					// mando la richiesta di aggiunta
					result = reqHandler.NewFriendship(target);
					if (result == 0)// richiesta a buon fine
						addFriend(target);
					break;
				}
				case 1: { // Crea Chat Room
					// mando la richiesta di creazione
					result = reqHandler.CreateChatroom(target);
					if (result == 0)// richiesta a buon fine
						addChatRoom(target);
					break;
				}
				case 2: { // Elimina Chat Room
					// mando la richiesta di eliminazione
					result = reqHandler.newCloseChatroom(target);
					if (result == 0)// richiesta a buon fine
						deleteChatroom(target);
					break;
				}
				case 3: { // Join Chat Room
					// mando la richiesta di join
					result = reqHandler.newJoinChatroom(target);
					if (result == 0)// richiesta a buon fine
						addChatRoom(target);
					break;
				}
				default:
					return; // ignoro
				}
				friendslist.repaint();
				displayDialogWindow(result);// mostro all'utente il risultato
											// dell'operazione
			}

		});
		String s[] = { "Get Friends List", "Get Chat Rooms List" };
		// operazioni di richiesta chatlist e friendlist
		JComboBox<String> lists = new JComboBox<String>(s);
		lists.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (lists.getSelectedIndex() == 0) {
					// get friendlist request
					String[] reply = reqHandler.getFriendsList();
					if (reply == null || reply.length == 0)
						displayDialogWindow(13);
					else {
						String list_of_friends = "";
						for (String s : reply) {
							list_of_friends = list_of_friends.concat(s);
							list_of_friends = list_of_friends.concat("\n");
						}
						// mostro gli amici
						JOptionPane.showMessageDialog(getContentPane(), list_of_friends, "I Tuoi Amici",
								JOptionPane.INFORMATION_MESSAGE);
					}
				} else {
					// get chatroomslist request
					String[] reply = reqHandler.listChatroom();
					if (reply == null || reply.length == 0)
						displayDialogWindow(14);
					else {
						String list_of_chats = "('Y' se appartieni al gruppo, 'N' altrimenti)\n";
						for (String s : reply) {
							list_of_chats = list_of_chats.concat(s);
							list_of_chats = list_of_chats.concat("\n");
						}
						// mostro gli amici
						JOptionPane.showMessageDialog(getContentPane(), list_of_chats, "I Gruppi Disponibili",
								JOptionPane.INFORMATION_MESSAGE);
					}
				}

			}
		});
		lists.setBounds(10, 560, 160, 30);

		// parte alta
		panel.add(searchBar);
		panel.add(searchBtn);
		panel.add(combo);
		panel.add(multipleActions);
		panel.add(performAction);

		// panels aggiuntivi
		panel.add(friendslist);
		panel.add(chats);
		// parte bassa
		panel.add(chat_input_field);
		panel.add(send);
		panel.add(sendFile);
		panel.add(chat_label);
		panel.add(lists);

		setVisible(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	public void setClientHandler(ClientOps co) {
		this.reqHandler = co;
	}

	// Qua iniziera' una serie di metodi per la gestione di eventi nella GUI,
	// come l'arrivo di un nuovo messaggio, l'aggiunta di un'amicizia e cosi
	// via.
	// Questi metodi hanno dunque il compito di mostrare all'utente le
	// interazioni dinamiche con il server e con gli altri utenti

	/**
	 * Aggiunge un utente alla lista di utenti online mostrata sulla destra
	 * dell'interfaccia
	 * 
	 * @param username
	 */
	public void addFriend(String username) {
		// se l'amico e' gia' presente ignoriamo la richiesta
		if (myFriends.containsKey(username) || username == null)
			return;

		JLabel label = new JLabel(username);
		label.setBorder(BorderFactory.createEtchedBorder());
		label.setName(username);
		label.setBounds(10, 30 * friendscounter + 35, 150, 30);

		// clickando su un amico aggiugno una chat
		label.addMouseListener(new MouseListener() {
			public void mouseClicked(java.awt.event.MouseEvent arg0) {
				boolean found = false;
				// aggiungo una nuova chat solo se non e' gia' presente
				for (Component a : chats.getComponents()) {
					if (((JButton) a).getText().equals(username))
						found = true;
				}
				if (!found)
					newChat(username);
			}

			public void mouseEntered(java.awt.event.MouseEvent arg0) {
			}

			public void mouseExited(java.awt.event.MouseEvent arg0) {
			}

			public void mousePressed(java.awt.event.MouseEvent arg0) {
			}

			public void mouseReleased(java.awt.event.MouseEvent arg0) {
			}
		});

		myFriends.put(username, status.online);
		friendscounter++;
		friendslist.add(label);
		getContentPane().revalidate();

	}

	/**
	 * Funzione che aggiunge alla vista una nuova chatroom
	 * 
	 * @param idchat
	 */
	public void addChatRoom(String idchat) {
		// se la chatroom e' gia' presente ignoriamo la richiesta
		if (myChatRooms.contains(idchat))
			return;

		JLabel label = new JLabel(idchat);
		label.setBorder(BorderFactory.createEtchedBorder());
		label.setName(idchat);
		label.setBounds(10, 30 * chatroomscounter + 435, 150, 30);

		// clickando sulla chatroom apro la chat
		label.addMouseListener(new MouseListener() {
			public void mouseClicked(java.awt.event.MouseEvent arg0) {
				boolean found = false;
				// aggiungo una nuova chat solo se non e' gia' presente
				for (Component a : chats.getComponents()) {
					if (((JButton) a).getText().equals(idchat))
						found = true;
				}
				if (!found)
					newChat(idchat);
			}

			public void mouseEntered(java.awt.event.MouseEvent arg0) {
			}

			public void mouseExited(java.awt.event.MouseEvent arg0) {
			}

			public void mousePressed(java.awt.event.MouseEvent arg0) {
			}

			public void mouseReleased(java.awt.event.MouseEvent arg0) {
			}
		});

		myChatRooms.add(idchat);
		chatroomscounter++;
		friendslist.add(label);
		getContentPane().revalidate();
	}

	/**
	 * Aggiunge un 'bottone' chat nella barra predisposta in basso, su cui
	 * l'utente puo clickare per conversare con un amico
	 * 
	 * @param username
	 *            dell'amico con cui vogliamo chattare
	 */
	public void newChat(String username) {
		// creo una nuova chat
		JButton chat = new JButton(username);
		chat.setSize(new Dimension(50, 100));
		chat.setBorder(BorderFactory.createEtchedBorder());
		chat.setName(username);

		// clickando su una chat la apro
		chat.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				// cerco la chat corrispondente al bottone premuto
				boolean found = false;
				int active = 0;
				for (JTextArea a : chat_area) {
					if (a == null) {
						// chat area non inizializzata
						break;
					} else if (a.getName().equals(username)) {
						// chat trovata

						a.setVisible(true);
						found = true;
						activeChatArea = active;
					} else {
						// nascondo le altre chat
						a.setVisible(false);
						active++;
					}
				}

				// se non e' stata trovata, significa che bisogna crearla
				if (!found) {
					chat_area[active] = new JTextArea();
					JTextArea a = chat_area[active];
					activeChatArea = active;
					// set up della textArea
					a.setName(username);
					a.setEditable(false);

					// JScrollPane scroll = new JScrollPane(a);
					// scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

					a.setBorder(BorderFactory.createTitledBorder(username));
					a.setLineWrap(true);
					a.setBounds(50, 70, getSize().width - 350, getSize().height - 250);
					getContentPane().add(a);
					a.setVisible(true);
					getContentPane().repaint();
				}
				getContentPane().revalidate();

			}

		});

		chats.add(chat);
		getContentPane().revalidate();
	}

	/**
	 * Funzione di utility utilizzata per mostrare un messaggio variabile al
	 * seguito del completamento di un'operazione, in un pop-up che informera'
	 * l'utente di quello che sta succedendo; puo' essere richiamata anche dal
	 * listener, in caso di eventi in background per notificare l'utente di un
	 * file ricevuto o di un nuovo messaggio.
	 * 
	 * @param msgIndex
	 */
	public void displayDialogWindow(int msgIndex) {

		final String messages[] = { "Operazione eseguita con successo!", "Username non registrato!",
				"Utente selezionato non e' un amico!", "Utente destinatario offline!", "Username/Chatname gia' in uso!",
				"File non esistente!", "Chat Room non registrata!", "Non sei registrato alla Chat Room!",
				"Nessun utente e' al momento online nella Chat Room!", "Fai gia' parte di questa Chat Room!",
				// messaggi di informazione
				"File inviato correttamente!", "Hai ricevuto un nuovo file!", "Hai ricevuto un nuovo messaggio!",
				"Non hai amici! Aggiungine uno dalla barra in alto",
				"Non c'e' alcun gruppo! Creane uno dalla barra in alto", "Utente Online" , "Utente Offline" };

		String msg;
		// assegniamo al messaggio da mostrare un contenuto
		if (msgIndex == -1)
			msg = "Operazione Fallita!";
		else
			msg = messages[msgIndex];

		// dialog message
		if (msgIndex == 0 || msgIndex == 15 || msgIndex == 16) {
			JOptionPane.showMessageDialog(this, msg, "Operazione eseguita", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this, msg, "Errore", JOptionPane.ERROR_MESSAGE);
		}

	}

	/**
	 * Funzione che si occupa di aggiornare la vista nel caso arrivi un nuovo
	 * messaggio; sara' infatti il listener che si occupera' di ascoltare nuovi
	 * eventuali messaggi in arrivo, e dopodiche aggiornare la chat
	 * 
	 * @param sender
	 * @param text
	 */
	public void newMessage(String sender, String text) {
		boolean found = false;
		// prima cerco il chat button fra quelli gia' presenti
		for (Component a : chats.getComponents()) {
			if (((JButton) a).getText().equals(sender))
				found = true;
		}
		if (!found)
			newChat(sender); // se non c'e' lo creo

		// apro la chat relativa al sender
		found = false;
		int active = 0;
		for (JTextArea a : chat_area) {
			if (a == null) {
				// chat area non inizializzata
				break;
			} else if (a.getName().equals(sender)) {
				// chat trovata
				
				// aggiungo il nuovo messaggio
				a.append("\n" + "[" + sender + "]: " + text); 
				a.setVisible(true);
				found = true;
				activeChatArea = active;
			} else {
				// nascondo le altre chat
				a.setVisible(false);
				active++;
			}
		}

		// se non e' stata trovata, significa che bisogna crearla
		if (!found) {
			chat_area[active] = new JTextArea();
			JTextArea a = chat_area[active];
			activeChatArea = active;
			// set up della textArea
			a.setName(sender);
			a.setEditable(false);
			// aggiungo il nuovo messaggio
			a.append("\n" + "[" + sender + "]: " + text); 

			a.setBorder(BorderFactory.createTitledBorder(sender));
			a.setLineWrap(true);
			a.setBounds(50, 70, getSize().width - 350, getSize().height - 250);
			getContentPane().add(a);
			a.setVisible(true);
			getContentPane().repaint();
		}
		this.validate();
		getContentPane().revalidate();

		// nel caso non venga trovato alcun utente fra gli amici
		// ignoriamo la richiesta, senza generare errori inutili per l'utente
	}

	/**
	 * Funzione che si occupa di eliminare un amico dalla lista degli amici,
	 * chiudendo qualsiasi chat aperta con esso
	 * 
	 * @param username
	 */
	public void deleteFriend(String username) {
		// controllo se l'username e' fra i miei amici
		if (!myFriends.containsKey(username))
			return; // se non lo e' ignoro la richiesta

		myFriends.remove(username);
		friendscounter--;
		// rimuovo la JLabel corrispondente
		// dalla lista amici
		for (Component a : friendslist.getComponents()) {
			if (a == null)
				continue;
			a = (JLabel) a;
			if (a.getName().equals(username)) {
				// questo e' l'utente da eliminare
				friendslist.remove(a);
				friendslist.revalidate();
				friendslist.repaint();
				break;
			}
		}

		// chiudo anche la chat se presente
		for (Component a : chats.getComponents()) {
			if (a == null)
				continue;
			a = (JButton) a;
			if (a.getName().equals(username)) {
				// questo e' la chat da eliminare
				chats.remove(a);
				chats.revalidate();
				chats.repaint();
				break;
			}
		}

		for (JTextArea a : chat_area) {
			if (a == null)
				continue;
			if (a.getName().equals(username)) {
				// questo e' la chat da eliminare
				getContentPane().remove(a);
				getContentPane().revalidate();
				getContentPane().repaint();
				a = null;
				break;
			}
		}
	}

	/**
	 * Chiude la chat(se aperta) con questo gruppo e lo elimina dalle chatroom
	 * in lista
	 * 
	 * @param idchat
	 */
	public void deleteChatroom(String idchat) {
		// controllo se l'idchat e' riconosciuta
		if (!myChatRooms.contains(idchat))
			return; // se non lo e' ignoro la richiesta

		myChatRooms.remove(idchat);
		chatroomscounter--;
		// rimuovo la JLabel corrispondente
		// dalla lista amici
		for (Component a : friendslist.getComponents()) {
			if (a == null)continue;
			a = (JLabel) a;
			if (a.getName().equals(idchat)) {
				// questo e' l'utente da eliminare
				friendslist.remove(a);
				friendslist.revalidate();
				friendslist.repaint();
				break;
			}
		}

		// chiudo anche la chat se presente
		for (Component a : chats.getComponents()) {
			if (a == null)
				continue;
			a = (JButton) a;
			if (a.getName().equals(idchat)) {
				// questo e' la chat da eliminare
				chats.remove(a);
				chats.revalidate();
				chats.repaint();
				break;
			}
		}

		for (JTextArea a : chat_area) {
			if (a == null)
				continue;
			if (a.getName().equals(idchat)) {
				// questo e' la chat da eliminare
				getContentPane().remove(a);
				getContentPane().revalidate();
				getContentPane().repaint();
				a = null;
				break;
			}
		}
	}

	/**
	 * Funzione utilizzata per cambiare lo stato di un amico, visualizzato
	 * nell'interfaccia grafica dell'utente
	 * 
	 * @param username,
	 *            l'username dell'amico il cui stato e' cambiato
	 * @param userStatus,
	 *            0 rappresenta lo stato offline, 1 rappresenta lo stato online
	 */
	public void changeStatus(String username, int userStatus) {
		if (!myFriends.containsKey(username))
			return;

		// aggiorno il valore della JLabel relativa
		for (Component a : friendslist.getComponents()) {
			if (a == null)
				break;

			if (a.getName().equals(username)) {
				// trovato la JLabel corrispondente
				if (userStatus == 0)
					((JLabel) a).setText(a.getName() + ":offline");
				else if (userStatus == 1)
					((JLabel) a).setText(a.getName() + ":online");
			}
		}

		if (userStatus == 0)// aggiorno il vecchio valore di status
			myFriends.put(username, status.offline); 
		else if (userStatus == 1)
			myFriends.put(username, status.online);
		// altrimenti ignoro la richiesta

		friendslist.revalidate();
		friendslist.repaint();
	}

}
