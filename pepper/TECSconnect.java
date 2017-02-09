package rise.connectors.pepper;

//import vpro.utils.tecs.*;
import rise.core.utils.tecs.*;


/**
 * Created by sander on 09/02/2017.
 */
public class TECSconnect {

	public static String behavior;
	public static String text2speech;

	public static TECSClient clientTECS = null;

	// public static void connectToTECS(final String clientName) {
	public static void connectToTECS(final String hostIP, final String clientName, final int port) {
		Thread inputListener = new Thread() {
			@Override
			public void run() {
				System.out.println("Connecting to TECS Server - " + hostIP + ":" + port);
				clientTECS = new TECSClient(hostIP, clientName, port);

				if (clientTECS != null) {
					clientTECS.subscribe(riseConstants.BehaviourMsg, palBhvEventHandler);

					clientTECS.startListening();
					System.out.println("Connected to TECS Server");

				}
			}
		};
		inputListener.start();
	}

	public void disconnectToTECS() {
		clientTECS.disconnect();
		if (clientTECS != null) {
			clientTECS = null;
		}

		System.out.println("Disconnected to TECS Server!");

	}

	protected static EventHandler<Behaviour> palBhvEventHandler = new EventHandler<Behaviour>() {
		public void handleEvent(Behaviour event) {
			// wait for processing until all is initialized

			behavior = event.getGesture();
			System.out.println(behavior);
			text2speech = event.getTextToSpeak();
			System.out.println(text2speech);

		}
	};

}
