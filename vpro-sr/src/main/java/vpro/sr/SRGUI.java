/**
* Speech Recognition GUI based on Google Speech API v2
*
* @author  ADEM F. IDRIZ
* @version 1.0
* @since  2016 - TU Delft
*/

package vpro.sr;

import java.io.File;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.darkprograms.speech.microphone.Microphone;

import net.sourceforge.javaflacencoder.FLACFileWriter;

import rise.core.utils.tecs.*;
//import rise.core.utils.tecs.Metadata;
//import rise.core.utils.tecs.TECSClient;
import vpro.sr.RecognizeSpeech;

public class SRGUI extends javax.swing.JFrame {
	
	
	/* RobotPresenterData */
	int structure_prepositional_phrases_count = 0;
	int structure_adjective_count = 0;
	int structure_non_future = 0;
	int structure_active_sentences = 0;
	int structure_personal_pronouns = 0;
	int structure_word_length = 0;
	int structure_negations = 0;
	int structure_adverbs = 0;
	int structure_passive_sentences = 0;
	int structure_number_of_sentences = 0;
	int structure_future = 0;
	int structure_wordcount = 0;
	String metadata_speaker = "";
	String metadata_lang = "";
	String metadata_timestamp = "";
	String metadata_input_text = "";
	String metadata_confidence_score = "";
	List<String> metadata_other_possible_responses = new ArrayList<String>();
	List<String> metadata_people_present = new ArrayList<String>();
	String metadata_script_id = "";
	List<Double> metadata_location_geometry_coordinates = new ArrayList<Double>();
	String metadata_location_geometry_type = "";
	String metadata_location_type = "";
	List<String> metadata_location_properties_name = new ArrayList<String>();
	String metadata_scene_id = "";
	List<String> semantic_keywords = new ArrayList<String>();
	List<String> semantic_organisations = new ArrayList<String>();
	List<String> semantic_people = new ArrayList<String>();
	List<String> semantic_places = new ArrayList<String>();
	List<String> semantic_topics =new ArrayList<String>();
	List<String> emotions_detected_emotion = new ArrayList<String>();
	List<String> emotions_information_state = new ArrayList<String>();
	
	
//	/* ProcessedRobotPresenterData */
//	int structure_prepositional_phrases_count = 0;
//	int structure_adjective_count = 0;
//	int structure_non_future = 0;
//	int structure_active_sentences = 0;
//	int structure_personal_pronouns = 0;
//	int structure_word_length = 0;
//	int structure_negations = 0;
//	int structure_adverbs = 0;
//	int structure_passive_sentences = 0;
//	int structure_number_of_sentences = 0;
//	int structure_future = 0;
//	int structure_wordcount = 0;
//	String metadata_speaker = "Adem";
//	String metadata_lang = "English"; // required
//	String metadata_timestamp = "";
//	String metadata_input_text = "";
//	String metadata_confidence_score = "";
//	List<String> metadata_other_possible_responses = Arrays.asList("");
//	List<String> metadata_people_present =  Arrays.asList("Adem", "Nao", "Pepper"); // required
//	String metadata_script_id = "episode_1";
//	List<Double> metadata_location_geometry_coordinates = Arrays.asList(52.373309, 4.878257);
//	String metadata_location_geometry_type = "Point";
//	String metadata_location_type = "Feature";
//	List<String> metadata_location_properties_name = Arrays.asList("Amsterdam", "Netherlands");
//	String metadata_scene_id = "scene_1";
//	List<String> semantic_keywords = Arrays.asList("");
//	List<String> semantic_organisations = Arrays.asList("");
//	List<String> semantic_people = Arrays.asList("");
//	List<String> semantic_places = Arrays.asList("");
//	List<String> semantic_topics = Arrays.asList("");
//	List<String> emotions_detected_emotion = Arrays.asList("");
//	List<String> emotions_information_state = Arrays.asList("");

//	/* Metadata */
//	public String speaker = "Adem"; // required
//	public String lang = "English"; // required
//	public String timestamp = ""; // required
//	public String input_text = ""; // required
//	public List<String> people_present = Arrays.asList("Adem", "Nao", "Pepper"); // required
//	public String script_id = "episode_1"; // required
//	public List<Double> location_geometry_coordinates = Arrays.asList(52.373309, 4.878257); // required
//	public String location_geometry_type = "Point"; // required
//	public String location_type = "Feature"; // required
//	public List<String> location_properties_name = Arrays.asList("Amsterdam", "Netherlands"); // required
//	public String scene_id = "scene_1"; // required

	// Google Speech API v2 Key
	static String apikey = "AIzaSyBOti4mM-6x9WDnZIjIeyEU21OpBXqWBgw";

	// Default
	public String RecordingLanguage = "English";
	public int MaxAlternativeNumber = 0;

	public static TECSClient clientTECS = null;

	private static String clientName = "SpeechRecognitionModule";
	private static String tecsserver = "127.0.0.1";
	private static int tecsPort = 9000;

	String newline = System.getProperty("line.separator");
	String workingDir = System.getProperty("user.dir");

	String icon_record = "record-icon-48.png";
	String icon_mic = "microphone-48.png";
	String icon_connect = "connect48.png";
	String icon_stop = "stop48.png";
	String icon_connected = "connected48.png";

	protected boolean connectedtoTECS = false;

	Microphone mic = new Microphone(FLACFileWriter.FLAC);
	File file = new File(workingDir + "/recording.flac"); // Name your file
															// whatever you want

	public void record() {

		mic.open();

		try {
			mic.captureAudioToFile(file);
		} catch (Exception ex) {
			// Microphone not available or some other error.
			System.err.println("ERROR: Microphone is not availible.");
			ex.printStackTrace();
		}
		System.out.println("Recording...");

	}

	public void stopRecord() {

		mic.close(); // Ends recording and frees the resources
		System.out.println("Recording completed.");
		System.out.println("Recognizing...");
		NotificationsTextArea.append("Recognizing..." + newline);
		RecognizeSpeech.Recognize(file, RecordingLanguage, apikey, MaxAlternativeNumber,
				(int) mic.getAudioFormat().getSampleRate());

	}

	// public static void connectToTECS(final String clientName) {
	public void connectToTECS(String hostIP, String clientName, int port) {

		NotificationsTextArea.append("Connecting to TECS Server - " + tecsserver + ":" + tecsPort + newline);
		clientTECS = new TECSClient(hostIP, clientName, port);

		if (clientTECS != null) {
			 clientTECS.subscribe(riseConstants.ProcessedRobotPresenterDataMsg,
					 ProcessedRobotPresenterDataEventHandler);

			clientTECS.startListening();
			// connectedtoTECS = clientTECS.isConnected();
			connectedtoTECS = true;
		}

		if (connectedtoTECS) {
			NotificationsTextArea.append("Connected to TECS Server " + newline);
			NotificationsTextArea.append("------------------------------------------------------------------------------"  + newline);

			ConnectButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon_connected)));
		}

	}

	public void disconnectToTECS() {
		clientTECS.disconnect();
		if (clientTECS != null) {
			connectedtoTECS = false;
			clientTECS = null;
		}
		// connectedtoTECS = clientTECS.isConnected();
		NotificationsTextArea.append("Disconnected! " + newline);
		ConnectButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon_connect)));
		NotificationsTextArea.append("------------------------------------------------------------------------------"  + newline);


	}
	
	
	EventHandler<ProcessedRobotPresenterData> ProcessedRobotPresenterDataEventHandler = new EventHandler<ProcessedRobotPresenterData>() {
		public void handleEvent(ProcessedRobotPresenterData event) {

			// Parsing Messages

			structure_prepositional_phrases_count = ((ProcessedRobotPresenterData) event).structure_prepositional_phrases_count;
			structure_adjective_count = ((ProcessedRobotPresenterData) event).structure_adjective_count;
			structure_non_future = ((ProcessedRobotPresenterData) event).structure_non_future;
			structure_active_sentences = ((ProcessedRobotPresenterData) event).structure_active_sentences;
			structure_personal_pronouns = ((ProcessedRobotPresenterData) event).structure_personal_pronouns;
			structure_word_length = ((ProcessedRobotPresenterData) event).structure_word_length;
			structure_negations = ((ProcessedRobotPresenterData) event).structure_negations;
			structure_adverbs = ((ProcessedRobotPresenterData) event).structure_adverbs;
			structure_passive_sentences = ((ProcessedRobotPresenterData) event).structure_passive_sentences;
			structure_number_of_sentences = ((ProcessedRobotPresenterData) event).structure_number_of_sentences;
			structure_future = ((ProcessedRobotPresenterData) event).structure_future;
			structure_wordcount = ((ProcessedRobotPresenterData) event).structure_wordcount;
			metadata_speaker = ((ProcessedRobotPresenterData) event).metadata_speaker;
			metadata_lang = ((ProcessedRobotPresenterData) event).metadata_lang;
			metadata_timestamp = ((ProcessedRobotPresenterData) event).metadata_timestamp;
			metadata_input_text = ((ProcessedRobotPresenterData) event).metadata_input_text;
			metadata_confidence_score = ((ProcessedRobotPresenterData) event).metadata_confidence_score;
			metadata_other_possible_responses = ((ProcessedRobotPresenterData) event).metadata_other_possible_responses;
			metadata_people_present = ((ProcessedRobotPresenterData) event).metadata_people_present;
			metadata_script_id = ((ProcessedRobotPresenterData) event).metadata_script_id;
			metadata_location_geometry_coordinates = ((ProcessedRobotPresenterData) event).metadata_location_geometry_coordinates;
			metadata_location_geometry_type = ((ProcessedRobotPresenterData) event).metadata_location_geometry_type;
			metadata_location_type = ((ProcessedRobotPresenterData) event).metadata_location_type;
			metadata_location_properties_name = ((ProcessedRobotPresenterData) event).metadata_location_properties_name;
			metadata_scene_id = ((ProcessedRobotPresenterData) event).metadata_scene_id;
			semantic_keywords = ((ProcessedRobotPresenterData) event).semantic_keywords;
			semantic_organisations = ((ProcessedRobotPresenterData) event).semantic_organisations;
			semantic_people = ((ProcessedRobotPresenterData) event).semantic_people;
			semantic_places = ((ProcessedRobotPresenterData) event).semantic_places;
			semantic_topics = ((ProcessedRobotPresenterData) event).semantic_topics;
			emotions_detected_emotion = ((ProcessedRobotPresenterData) event).emotions_detected_emotion;
			emotions_information_state = ((ProcessedRobotPresenterData) event).emotions_information_state;

			
			NotificationsTextArea.append("***************************Processed Data*******************************"  + newline);

			/* Display Metadata */
			NotificationsTextArea.append("Speaker: '" + metadata_speaker + "'" + newline);
			NotificationsTextArea.append("Language: '" + metadata_lang + "'" + newline);
			NotificationsTextArea.append("Time: '" + metadata_timestamp + "'" + newline);
			NotificationsTextArea.append("Transcript: '" + metadata_input_text + "'" + newline);
			NotificationsTextArea.append("Confidence Score:" + metadata_confidence_score + "%" + newline);

			NotificationsTextArea.append("Other Possible Responses: " + newline);

			for (String s : metadata_other_possible_responses) {
				NotificationsTextArea.append(s + newline);
			}

			NotificationsTextArea.append("People present: " + newline);

			for (String s : metadata_people_present) {
				NotificationsTextArea.append(s + newline);
			}

			NotificationsTextArea.append("Script ID: '" + metadata_script_id + "'" + newline);

			NotificationsTextArea.append("Location Geometry Coordinates: " + newline);

			for (Double d : metadata_location_geometry_coordinates) {
				NotificationsTextArea.append(d + newline);
			}

			NotificationsTextArea
					.append("Location Geometry Type: '" + metadata_location_geometry_type + "'" + newline);

			NotificationsTextArea.append("Location Type: '" + metadata_location_type + "'" + newline);

			NotificationsTextArea.append("Location Properties Name: " + newline);

			for (String s : metadata_location_properties_name) {
				NotificationsTextArea.append(s + newline);
			}

			NotificationsTextArea.append("Scene ID: '" + metadata_scene_id + "'" + newline);

			/* Display Semantic */
			
			NotificationsTextArea.append("Semantic Keyword(s): '" + semantic_keywords + "'" + newline);

			NotificationsTextArea.append("Semantic Topic(s): " + semantic_topics  + newline);
			
			NotificationsTextArea.append("******************************************************************************"  + newline);


		}
	};
	

	/* Sending Recognized Speech to server */

	public void sendRecognizedSpeech(String results) {
		
		
		metadata_people_present.clear();
		metadata_location_geometry_coordinates.clear();
		metadata_location_properties_name.clear();
		

		/* Metadata */
		metadata_speaker = "Adem";
		metadata_lang = RecordingLanguage;
		
		Timestamp stamp = new Timestamp(System.currentTimeMillis());
		metadata_timestamp = stamp.toString();
		
		metadata_input_text= RecognizeSpeech.Transcript;
		metadata_confidence_score= RecognizeSpeech.ConfidenceScore;
		metadata_other_possible_responses= RecognizeSpeech.OtherPossibleResponses;
		
		
		metadata_people_present.add("Adem");
		metadata_people_present.add("Nao");
		metadata_people_present.add("Pepper");
		
		metadata_script_id = "episode_1"; 
		
		metadata_location_geometry_coordinates.add(52.373309);
		metadata_location_geometry_coordinates.add(4.878257);
		
		metadata_location_geometry_type = "Point";
		metadata_location_type = "Feature";
		
		metadata_location_properties_name.add("Amsterdam");
		metadata_location_properties_name.add("Netherlands");
		
		metadata_scene_id = "scene_1";
		

//		public List<String> people_present = Arrays.asList("Adem", "Nao", "Pepper"); // required
//		public List<Double> location_geometry_coordinates = Arrays.asList(52.373309, 4.878257); // required
//		public List<String> location_properties_name = Arrays.asList("Amsterdam", "Netherlands"); // required
//	
//		
	
		

		
//		clientTECS.send(new Metadata(speaker, lang, timestamp, results, people_present, script_id,
//				location_geometry_coordinates, location_geometry_type, location_type, location_properties_name,
//				scene_id));

		// clientTECS.send(new Speech(101, results, confidence));

		
		
		clientTECS.send(new RobotPresenterData(structure_prepositional_phrases_count,
				structure_adjective_count, structure_non_future, structure_active_sentences,
				structure_personal_pronouns, structure_word_length, structure_negations, structure_adverbs,
				structure_passive_sentences, structure_number_of_sentences, structure_future, structure_wordcount,
				metadata_speaker, metadata_lang, metadata_timestamp, metadata_input_text, metadata_confidence_score,
				metadata_other_possible_responses, metadata_people_present, metadata_script_id,
				metadata_location_geometry_coordinates, metadata_location_geometry_type, metadata_location_type,
				metadata_location_properties_name, metadata_scene_id, semantic_keywords, semantic_organisations,
				semantic_people, semantic_places, semantic_topics, emotions_detected_emotion,
				emotions_information_state));

		System.out.println("Recognized Speech is transmitted");
		
		NotificationsTextArea.append("Recognized Speech is transmitted" + newline);
		
		NotificationsTextArea.append("------------------------------------------------------------------------------"  + newline);


	}

	/**
	 * Creates new form ContactEditor
	 */
	public SRGUI() {

		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		ConnectionPanel = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		TECSServerIPAddressTextField = new javax.swing.JTextField();
		ConnectButton = new javax.swing.JButton();
		TECSServerPortTextField = new javax.swing.JTextField();
		jLabel2 = new javax.swing.JLabel();
		SpeechRecognitionPanel = new javax.swing.JPanel();
		jLabel6 = new javax.swing.JLabel();
		LanguageComboBox = new javax.swing.JComboBox();
		jLabel7 = new javax.swing.JLabel();
		MaxAlternativesTextField = new javax.swing.JTextField();
		RecordButton = new javax.swing.JButton();
		StopButton = new javax.swing.JButton();
		NotificationsPanel = new javax.swing.JPanel();
		NotificationsTextArea = new java.awt.TextArea();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Speech Recognition Module");

		ConnectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Connection",
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
				new java.awt.Font("Tahoma", 0, 14))); // NOI18N

		jLabel1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		jLabel1.setText("TECS Server Port:");

		TECSServerIPAddressTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		TECSServerIPAddressTextField.setText("127.0.0.1");
		TECSServerIPAddressTextField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TECSServerIPAddressTextFieldActionPerformed(evt);
			}
		});

		ConnectButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		ConnectButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("connect48.png")));
		ConnectButton.setText("Connect");
		ConnectButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ConnectButtonActionPerformed(evt);
			}
		});

		TECSServerPortTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		TECSServerPortTextField.setText("9000");
		TECSServerPortTextField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TECSServerPortTextFieldActionPerformed(evt);
			}
		});

		jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		jLabel2.setText("TECS Server IP Address:");

		org.jdesktop.layout.GroupLayout ConnectionPanelLayout = new org.jdesktop.layout.GroupLayout(ConnectionPanel);
		ConnectionPanel.setLayout(ConnectionPanelLayout);
		ConnectionPanelLayout.setHorizontalGroup(ConnectionPanelLayout
				.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(ConnectionPanelLayout.createSequentialGroup().addContainerGap()
						.add(ConnectionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(jLabel2).add(jLabel1))
						.add(18, 18, 18)
						.add(ConnectionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(TECSServerIPAddressTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 125,
										org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
								.add(TECSServerPortTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50,
										org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(ConnectButton)
						.addContainerGap()));
		ConnectionPanelLayout
				.setVerticalGroup(ConnectionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
						.add(ConnectionPanelLayout.createSequentialGroup().addContainerGap().add(
								ConnectionPanelLayout
										.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(
												ConnectionPanelLayout.createSequentialGroup()
														.add(ConnectionPanelLayout
																.createParallelGroup(
																		org.jdesktop.layout.GroupLayout.BASELINE)
																.add(jLabel2).add(TECSServerIPAddressTextField,
																		org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																		org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																		org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
														.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 13,
																Short.MAX_VALUE)
														.add(ConnectionPanelLayout
																.createParallelGroup(
																		org.jdesktop.layout.GroupLayout.BASELINE)
																.add(jLabel1).add(TECSServerPortTextField,
																		org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																		org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																		org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
										.add(ConnectionPanelLayout.createSequentialGroup()
												.add(ConnectButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45,
														org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
												.add(0, 0, Short.MAX_VALUE)))
								.addContainerGap()));

		SpeechRecognitionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Speech Recognition",
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
				new java.awt.Font("Tahoma", 0, 14))); // NOI18N
		SpeechRecognitionPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		SpeechRecognitionPanel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		SpeechRecognitionPanel.setName(""); // NOI18N
		SpeechRecognitionPanel.setOpaque(false);

		jLabel6.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		jLabel6.setText("Max Number of Alternatives:");

		LanguageComboBox.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		LanguageComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "English", "Dutch", "Auto" }));
		LanguageComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				LanguageComboBoxActionPerformed(evt);
			}
		});

		jLabel7.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		jLabel7.setText("Language:");

		MaxAlternativesTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
		MaxAlternativesTextField.setText("100");
		MaxAlternativesTextField.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				MaxAlternativesTextFieldActionPerformed(evt);
			}
		});

		RecordButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon_mic)));
		RecordButton.setRequestFocusEnabled(false);

		RecordButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource(icon_record)));
		RecordButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource(icon_record)));

		RecordButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				RecordButtonActionPerformed(evt);
			}
		});


		StopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon_stop)));
		StopButton.setRequestFocusEnabled(false);
		StopButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				StopButtonActionPerformed(evt);
			}
		});

		org.jdesktop.layout.GroupLayout SpeechRecognitionPanelLayout = new org.jdesktop.layout.GroupLayout(
				SpeechRecognitionPanel);
		SpeechRecognitionPanel.setLayout(SpeechRecognitionPanelLayout);
		SpeechRecognitionPanelLayout.setHorizontalGroup(SpeechRecognitionPanelLayout
				.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(SpeechRecognitionPanelLayout.createSequentialGroup().addContainerGap()
						.add(SpeechRecognitionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(SpeechRecognitionPanelLayout.createSequentialGroup().add(jLabel7).add(18, 18, 18)
										.add(LanguageComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
								.add(SpeechRecognitionPanelLayout.createSequentialGroup()
										.add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 181,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										.add(MaxAlternativesTextField)))
						.add(18, 18, 18).add(RecordButton).add(18, 18, 18).add(StopButton)
						.addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		SpeechRecognitionPanelLayout.setVerticalGroup(SpeechRecognitionPanelLayout
				.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(SpeechRecognitionPanelLayout.createSequentialGroup()
						.addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.add(SpeechRecognitionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(StopButton).add(RecordButton).add(
										SpeechRecognitionPanelLayout.createSequentialGroup()
												.add(SpeechRecognitionPanelLayout
														.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel6).add(MaxAlternativesTextField,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
												.add(18, 18, 18)
												.add(SpeechRecognitionPanelLayout
														.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel7).add(LanguageComboBox,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))));

		NotificationsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Notifications",
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
				new java.awt.Font("Tahoma", 0, 14))); // NOI18N
		NotificationsPanel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

		NotificationsTextArea.setMinimumSize(new java.awt.Dimension(100, 72));
		NotificationsTextArea.setPreferredSize(new java.awt.Dimension(100, 78));

		org.jdesktop.layout.GroupLayout NotificationsPanelLayout = new org.jdesktop.layout.GroupLayout(
				NotificationsPanel);
		NotificationsPanel.setLayout(NotificationsPanelLayout);
		NotificationsPanelLayout.setHorizontalGroup(
				NotificationsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
						.add(NotificationsPanelLayout.createSequentialGroup().addContainerGap()
								.add(NotificationsTextArea, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
										org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addContainerGap()));
		NotificationsPanelLayout.setVerticalGroup(NotificationsPanelLayout
				.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(NotificationsPanelLayout.createSequentialGroup().addContainerGap()
						.add(NotificationsTextArea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
						.add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().addContainerGap()
								.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
										.add(NotificationsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.add(org.jdesktop.layout.GroupLayout.LEADING, SpeechRecognitionPanel,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.add(ConnectionPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))));
		layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout
				.createSequentialGroup().addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.add(ConnectionPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
						org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
				.add(SpeechRecognitionPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
						org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
				.add(4, 4, 4)
				.add(NotificationsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
						org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
				.add(10, 10, 10)));

		ConnectionPanel.getAccessibleContext().setAccessibleName("Connection");
		SpeechRecognitionPanel.getAccessibleContext().setAccessibleDescription("");
		NotificationsPanel.getAccessibleContext().setAccessibleName("Notifications");
		NotificationsPanel.getAccessibleContext().setAccessibleDescription("");

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void RecordButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_RecordButtonActionPerformed

		RecordingLanguage = LanguageComboBox.getSelectedItem().toString();
		MaxAlternativeNumber = Integer.parseInt(MaxAlternativesTextField.getText());

		NotificationsTextArea.append("Selected Language: " + RecordingLanguage + " - Maximum Other Possibility:"
				+ MaxAlternativeNumber + newline);
		NotificationsTextArea.append("Recording..." + newline);
		
		RecordButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon_record)));
		record();
	}// GEN-LAST:event_RecordButtonActionPerformed

	private void TECSServerIPAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_TECSServerIPAddressTextFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_TECSServerIPAddressTextFieldActionPerformed

	private void ConnectButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ConnectButtonActionPerformed
		tecsserver = TECSServerIPAddressTextField.getText();
		tecsPort = Integer.parseInt(TECSServerPortTextField.getText());

		if (!connectedtoTECS) {
			connectToTECS(tecsserver, clientName, tecsPort);
		} else
			disconnectToTECS();

	}// GEN-LAST:event_ConnectButtonActionPerformed

	private void TECSServerPortTextFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_TECSServerPortTextFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_TECSServerPortTextFieldActionPerformed

	private void LanguageComboBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_LanguageComboBoxActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_LanguageComboBoxActionPerformed

	private void MaxAlternativesTextFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_MaxAlternativesTextFieldActionPerformed
		// TODO add your handling code here:
	}// GEN-LAST:event_MaxAlternativesTextFieldActionPerformed

	private void StopButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_StopButtonActionPerformed

		NotificationsTextArea.append("Recording is completed!" + newline);
		RecordButton.setIcon(new javax.swing.ImageIcon(getClass().getResource(icon_mic)));

		stopRecord();

		if (connectedtoTECS) {
			// send Recognized Speech result to TECS Server
			sendRecognizedSpeech(RecognizeSpeech.CompactResults);

		}
	}// GEN-LAST:event_StopButtonActionPerformed

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting
		// code (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the
		 * default look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.
		 * html
		 */
		try {
			javax.swing.UIManager.LookAndFeelInfo[] installedLookAndFeels = javax.swing.UIManager
					.getInstalledLookAndFeels();
			for (int idx = 0; idx < installedLookAndFeels.length; idx++)
				if ("Nimbus".equals(installedLookAndFeels[idx].getName())) {
					javax.swing.UIManager.setLookAndFeel(installedLookAndFeels[idx].getClassName());
					break;
				}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(SRGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(SRGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(SRGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(SRGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		// </editor-fold>
		// </editor-fold>
		// </editor-fold>
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new SRGUI().setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton ConnectButton;
	private javax.swing.JPanel ConnectionPanel;
	private javax.swing.JComboBox LanguageComboBox;
	private javax.swing.JTextField MaxAlternativesTextField;
	private javax.swing.JPanel NotificationsPanel;
	public static java.awt.TextArea NotificationsTextArea;
	private javax.swing.JButton RecordButton;
	private javax.swing.JPanel SpeechRecognitionPanel;
	private javax.swing.JButton StopButton;
	private javax.swing.JTextField TECSServerIPAddressTextField;
	private javax.swing.JTextField TECSServerPortTextField;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	// End of variables declaration//GEN-END:variables

}
