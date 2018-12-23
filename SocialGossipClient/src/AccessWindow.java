import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * Classe che si occupa dell'interfaccia della finestra di accesso al servizio
 * di Social Gossip; inviera' dunque la richiesta di Login o Registrazione
 * utilizzando le funzioni in ClientOps, e si chiude solo dopo aver ricevuto una
 * risposta positiva a una delle due richieste, mostrando cosi la home di Social
 * Gossip.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 *
 */
public class AccessWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private String access_btn_text = "Registrati";
	// gestore delle richieste di login/registrazione
	private ClientOps reqHandler;

	public AccessWindow(SG_Home home, ClientOps co) {
		// alla chiusura di questo frame, lo nascondiamo
		// lasciando l'app in esecuzione
		setTitle("Benvenuto in Social Gossip");
		setSize(400, 650);
		setLocation(600, 100);
		setResizable(false);
		reqHandler = co;

		// creo gli elementi che faranno parte della GUI per il login
		JPanel panel = (JPanel) getContentPane();
		// posizionamento degli elementi senza layout predefinito
		panel.setLayout(null); 

		// 2 label e 2 textfield, piu un bottone per il login
		JLabel username_label = new JLabel("Username");
		username_label.setBounds(getSize().width / 2 - 60, 150, 100, 30);

		JTextField username = new JTextField();
		username.setBounds(getSize().width / 2 - 100, 200, 200, 30);

		JLabel language_label = new JLabel("Lingua");
		language_label.setBounds(getSize().width / 2 - 60, 300, 100, 30);

		String languages[] = { "Italiano", "English", "Francais", "Deutsch", "Espanol", "Chinese" };

		JComboBox<String> language = new JComboBox<String>(languages);
		language.setBounds(getSize().width / 2 - 100, 350, 200, 30);

		// login/register button
		JButton accessbtn = new JButton(access_btn_text);
		// se il bottone viene premuto, eseguo la richiesta di login
		accessbtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// faccio la richiesta di login/registrazione
				String nickname = username.getText();
				if (nickname.equals("") || nickname == null)
					return;
				int result;
				if (accessbtn.getText().equals("Login")) {
					// login
					result = reqHandler.LoginUser(nickname);
				} else {
					// registrazione
					String lan = (String) language.getSelectedItem();
					System.out.println("LANGUAGE  " + lan);
					result = reqHandler.RegisterRequest(nickname, lan);
				}
				// richiesta completata, chiudo la finestra
				// di login corrente e apro la finestra di SocialGossip
				if (result == 0) {
					home.setTitle(reqHandler.myUsername);
					home.setVisible(true);
					dispose();
				} else
					displayDialogWindow(result);
			}
		});
		accessbtn.setBounds(getSize().width / 2 - 75, 500, 150, 50);

		JButton changeModebtn = new JButton("Login/Registrati");

		changeModebtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// cambio da login a registrazione
				if (accessbtn.getText().equals("Login")) {
					accessbtn.setText("Registrati");
					language_label.setVisible(true);
					language.setVisible(true);
				} else {
					accessbtn.setText("Login");
					language_label.setVisible(false);
					language.setVisible(false);
				}
				getContentPane().revalidate();
				getContentPane().repaint();
			}
		});
		changeModebtn.setBounds(getSize().width / 2 - 75, 10, 150, 50);

		// aggiungo gli elementi al panel
		panel.add(changeModebtn);
		panel.add(username_label);
		panel.add(username);
		panel.add(language_label);
		panel.add(language);
		panel.add(accessbtn);

		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	/**
	 * Funzione di utility utilizzata per mostrare un messaggio variabile al
	 * seguito del completamento di un'operazione, in un pop-up che informera'
	 * l'utente di quello che sta succedendo.
	 * 
	 * @param msgIndex,
	 *            indice del messaggio da mostrare
	 */
	public void displayDialogWindow(int msgIndex) {

		final String messages[] = { "Operazione eseguita con successo!", "Username gia' registrato!",
				"Username non riconosciuto!" };

		String msg;
		// assegniamo al messaggio da mostrare un contenuto
		if (msgIndex == -1) 
			msg = "Operazione Fallita!";
		else
			msg = messages[msgIndex];

		// dialog message
		if (msgIndex == 0) {
			JOptionPane.showMessageDialog(this, msg, "Operazione eseguita", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this, msg, "Errore", JOptionPane.ERROR_MESSAGE);
		}

	}
}
