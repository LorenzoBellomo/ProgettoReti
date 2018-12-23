import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.Vector;

import condivise.Notifier;

/**
 * Classe che implementa le notifiche di tipo RMI per il cambio di stato di
 * amici di un utente.
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public class RMINotifier extends RemoteObject implements Notifier {

	private static final long serialVersionUID = 1L;

	// il vector di user online e offline
	private Vector<String> online;
	private Vector<String> offline;

	// l'oggetto resposabile della GUI
	private SG_Home home;

	public RMINotifier(Vector<String> online, Vector<String> offline, SG_Home home) throws RemoteException {
		// COSTRUTTORE
		super(); // di RemoteObject
		this.online = online;
		this.offline = offline;
		this.home = home;
	}

	/**
	 * Metodi che implementano le funzioni RMI di notifica al client del
	 * cambiamento di stato di un amico
	 */
	public void NotifyOnlineFriend(String nickname) throws RemoteException {

		if (!this.offline.remove(nickname)) {

			// nuovo amico, non era presente prima nel vettore offline
			// aggiungo l'amico nell'interfaccia
			home.addFriend(nickname);
		}
		// notifico la GUI del cambiamento di stato
		home.changeStatus(nickname, 1);
		// aggiungo l'amico anche al vettore degli amici online
		this.online.add(nickname);
	}

	public void NotifyOfflineFriend(String nickname) throws RemoteException {

		if (!this.online.remove(nickname)) {
			// nuovo amico, non era presente prima nel vettore online
			// aggiungo l'amico nell'interfaccia
			home.addFriend(nickname);
		}
		// notifico la GUI del cambiamento di stato
		home.changeStatus(nickname, 0);
		// aggiungo l'amico anche al vettore degli amici offline
		this.offline.add(nickname);
	}

}
