/**
* Voice Command Detector based on ALSpeechRecognition
*
* @author  ADEM F. IDRIZ
* @version 1.0
* @since  2016 - TU Delft
*/

package rise.voicecommand;

import java.util.ArrayList;
import java.util.List;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;

import rise.connectors.nao.NAOConnector;
import rise.core.utils.tecs.EventHandler;
import rise.core.utils.tecs.GetVoiceCommand;
import rise.core.utils.tecs.VoiceCommand;

public class VoiceCommandDetector extends Thread {

	// Default
	public static String VoiceCommandWord = "Start";
	// public static String VoiceCommandLanguage = "English";
	public static String VoiceCommandLanguage = "Dutch";
	public static String MinThresholdLevel = "";

	public double FinalPrecision;
	public String FinalVoiceCommand;

	private ALMemory alMemory;
	private ALSpeechRecognition alSpeechRecognition;

	List<Integer> ThresholdLevels = new ArrayList<Integer>();
	ArrayList<String> listOfWords = new ArrayList<String>();

	public static String[] VCWords;
	public static String[] VCThresholds;

	private final String WORD_RECOGNIZED = "WordRecognized";

	protected static boolean shouldRun = false;
	// protected static boolean shouldRun = true;
	protected static boolean getVC = false;
	protected static boolean sendVC = false;

	private long eventID;

	public VoiceCommandDetector(com.aldebaran.qi.Session session) {
		try {

			this.alMemory = new ALMemory(session);
			this.alSpeechRecognition = new ALSpeechRecognition(session);

			listOfWords.add(VoiceCommandWord);

			updateParameters(listOfWords, VoiceCommandLanguage);

			// Stop ASR
			alSpeechRecognition.pause(true);

			// Enables or disables the LED animations showing the state of the
			// recognition engine during the recognition process.
			alSpeechRecognition.setVisualExpression(false);

			// Sets the LED animation mode: 0: deactivated, 1: eyes, 2: ears, 3:
			// full
			alSpeechRecognition.setVisualExpressionMode(0);

			// True, a “bip” is played at the beginning of the recognition
			// process, and another “bip” is played at the end of the process
			alSpeechRecognition.setAudioExpression(false);

			alSpeechRecognition.pause(false);

			alSpeechRecognition.subscribe(WORD_RECOGNIZED);

			eventID = alMemory.subscribeToEvent(WORD_RECOGNIZED, "onWordRecognized::(m)", this);

			shouldRun = true;

		} catch (Exception e) {
			System.out.println("Error creating Voice Command Detector thread");
			e.printStackTrace();
		}
	}

	public void run() {

		System.out.println("*************VoiceCommandDetector*******************");

		while (shouldRun) {
			try {
				sleep(50);
				@SuppressWarnings("unused")
				String workingDir = System.getProperty("user.dir");
	
				if (getVC) {
	
					getVC = false;
					sendVC = true;
	
					listOfWords.clear();
					ThresholdLevels.clear();
	
					for (String w : VCWords) {
	
						listOfWords.add(w);
					}
	
					for (String t : VCThresholds) {
	
						ThresholdLevels.add(Integer.parseInt(t));
					}
	
					for (String list : listOfWords) {
	
						System.out.println(list);
					}
	
					updateParameters(listOfWords, VoiceCommandLanguage);
	
				}
			} catch (Exception e){System.out.println(e.toString());}
		}
	}

	public void updateParameters(ArrayList<String> List, String Language) {
		try {

			// Stop ASR
			alSpeechRecognition.pause(true);
			// Update Vocabulary and Language
			alSpeechRecognition.setLanguage(Language);
			alSpeechRecognition.setVocabulary(List, false);
			alSpeechRecognition.pause(false);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopDetecting() {
		try {

			alSpeechRecognition.pause(true);
			alSpeechRecognition.unsubscribe(WORD_RECOGNIZED);
			alMemory.unsubscribeToEvent(eventID);
			alSpeechRecognition.pause(false);

			alSpeechRecognition.exit();

			shouldRun = false;

		} catch (Exception e) {
			System.out.println("Error closing VoiceCommandDetector thread");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void onWordRecognized(Object words) throws InterruptedException, CallError {

		String word = (String) ((List<Object>) words).get(0);
		double precision = (float) ((List<Object>) words).get(1) * 100;

		if (sendVC) {

			System.out.println(String.format("NAO heard: [ %s ] with %f precision.", word, precision));

			if (listOfWords.indexOf(word) != -1) {

				int RECOGNIZATION_THRESHOLD = ThresholdLevels.get(listOfWords.indexOf(word));

				if (precision > RECOGNIZATION_THRESHOLD) {

					FinalVoiceCommand = word;
					FinalPrecision = precision;

					sendVoiceCommand(FinalVoiceCommand, FinalPrecision);

				}

			}
		}
	}

	/* sending Voice Command to server */
	public void sendVoiceCommand(String command, double precision) {

		NAOConnector.sendToServer(new VoiceCommand(102, command, precision));

		System.out.println("Voice Command is transmitted");

		FinalVoiceCommand = "";
		FinalPrecision = 0;

		listOfWords.clear();
		ThresholdLevels.clear();

		sendVC = false;

	}

	// Handle type of GetVoiceCommand message
	public static EventHandler<GetVoiceCommand> getVoiceCommandEventHandler = new EventHandler<GetVoiceCommand>() {
		public void handleEvent(GetVoiceCommand event) {

			// Parsing Messages
			VoiceCommandWord = ((GetVoiceCommand) event).word;

			VoiceCommandLanguage = ((GetVoiceCommand) event).language;

			MinThresholdLevel = ((GetVoiceCommand) event).threshold;

			VCWords = VoiceCommandWord.split("&");

			VCThresholds = MinThresholdLevel.split("&");

			System.out
					.println("Voice Command Language: " + VoiceCommandLanguage + "  -  " + "Expected Voice Command(s): "
							+ VoiceCommandWord + "  -  " + "Minimum Level of threshold(s): " + MinThresholdLevel);

			getVC = true;

		}
	};

}
