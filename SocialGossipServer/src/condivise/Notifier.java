package condivise;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI fornita dal client al server per ottenere aggiornamenti 
 * su un cambiamento di stato di amici di un utente. 
 * Consta di due metodi , utilizzati per notificare un amico online o un
 * amico offline. 
 * 
 * @author Lorenzo Bellomo, Nicolo' Lucchesi
 */
public interface Notifier extends Remote {

	/**
	 * Il server notifica il client che un suo amico e' adesso online
	 * Il nickname inviato potrebbe potenzialmente essere sconosciuto al client.
	 * In tal caso si tratta di un nuovo client che ha richiesto l'amicizia del 
	 * client
	 * 
	 * @param nickname il nome dell'amico ora online
	 * @throws RemoteException
	 */
	public void NotifyOnlineFriend(String nickname) throws RemoteException;
	
	/**
	 * Il server notifica il client che un suo amico e' adesso offline
	 * 
	 * @param nickname il nome dell'amico ora offline
	 * @throws RemoteException
	 */
	public void NotifyOfflineFriend(String nickname) throws RemoteException;

}