package rise.connectors.nao;

import static rise.connectors.nao.NAOConstants.EXT_TTS_BEHAVIOR_ONNAO_PROP;
import static rise.connectors.nao.NAOConstants.EXT_TTS_HOST_PROP;
import static rise.connectors.nao.NAOConstants.LOCALIZATION_MOVE_SPEED_PROP;
import static rise.connectors.nao.NAOConstants.LOCALIZATION_SENSITIVITY_PROP;
import static rise.connectors.nao.NAOConstants.NAO_AUTO_CONNECT;
import static rise.connectors.nao.NAOConstants.NAO_CONFIG_FILE_NAME_PROP;
import static rise.connectors.nao.NAOConstants.NAO_NAME_PROP;
import static rise.connectors.nao.NAOConstants.SPEECH_PITCH_PROP;
import static rise.connectors.nao.NAOConstants.SPEECH_SPEED_PROP;
import static rise.connectors.nao.NAOConstants.SPEECH_VOLUME_PROP;
import static rise.connectors.nao.NAOConstants.USE_EXTERNAL_TTS_PROP;
import static rise.connectors.nao.NAOConstants.USE_LOCALIZATION_PROP;
import static rise.connectors.nao.NAOConstants.USE_ON_NAO;
import static rise.core.utils.Constants.NAO_IP_PROP;
import static rise.core.utils.Constants.TECSSERVER_IP_PROP;
import static rise.core.utils.Constants.TECSSERVER_PORT_PROP;
import rise.core.utils.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.thrift.TBase;

import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALAudioDevice;
import com.aldebaran.qi.helper.proxies.ALAutonomousLife;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;

import rise.core.utils.tecs.*;
import rise.speechrecognition.SpeechRecognition;
import rise.voicecommand.VoiceCommandDetector;

public final class NAOConnector {

  public static final String TOCROUCHED = "Crouch";
  public static final String TOSTANDUP = "Stand";
  public static final String TOSITDOWN = "Sit";

  public static final String READY="Ik ben klaar voor een taak.";
  public static final String CONNECTING="Ik ben wakker en probeer te praten met de server.";
  public static final String DISCONNECTED="Oeps. Ik kan niet meer praten met de server. Ik probeer het opnieuw.";
  public static final String CANTCONNECT="Er is een probleem. Ik kan de server niet vinden. Controleer of ik wel op jullie wifi zit door op mijn buik te drukken.";
  
    public static boolean TECSFromCommandLine=false;
  
  private static String[] necessaryNAOProperties = { NAO_NAME_PROP,
      USE_EXTERNAL_TTS_PROP, SPEECH_PITCH_PROP, SPEECH_SPEED_PROP,
      SPEECH_VOLUME_PROP, EXT_TTS_BEHAVIOR_ONNAO_PROP, NAO_AUTO_CONNECT,
      EXT_TTS_HOST_PROP, USE_LOCALIZATION_PROP, LOCALIZATION_MOVE_SPEED_PROP,
      LOCALIZATION_SENSITIVITY_PROP, USE_ON_NAO };

  private static ObjectListenerBase soundListener = new ObjectListenerBase() {
    @Override
    public void receive(final Object data) {
      // logger.info("NAOConnector.soundListener.catch ");
      processIncomingSoundListener(data);
    }
  };

  private static StringListenerBase ttsListener = new StringListenerBase() {
    @Override
    public void receive(final String text) {
      processNAOTTSMessage(text);
    }
  };

  private static StringListenerBase extTTSListener = new StringListenerBase() {
    @Override
    public void receive(final String text) {
      processIncomingTTSEngineMessage(text);
    }
  };

  private static StringListenerBase touchListener = new StringListenerBase() {
    @Override
    public void receive(final String text) {
      // logger.info("NAOConnector.touchListener.catch " + text);
    }
  };

  public static int hostPort = 9559;
  public static boolean connectionState = false;

  protected static boolean isTalking = false;
  protected static boolean isMoving = false;
  private static com.aldebaran.qi.Session session = null;
  private static com.aldebaran.qi.Future<Void> naoConnect = null;
  private static AnyObject tts = null;
  private static AnyObject behaviorMngr = null;
  private static AnyObject motion = null;
  private static AnyObject memory = null;
  private static AnyObject audioDevice = null;
  private static AnyObject posture = null;
  private static AnyObject leds = null;
  private static AnyObject locateSound = null;
  private static ALAutonomousLife aLife = null;

  //private static NAONoiseDetector detectNoise; //detect loud noises in the background
  
  private static VoiceCommandDetector detectVoiceCommand; // detect Voice Command
	
  private static SpeechRecognition speechRecognition; // Free Speech Recognition

  
  private static Object lockMotion = new Object();

  private static List<Listener<Integer>> stateChangeListeners = new ArrayList<>();

  public static void addStateChangeListener(Listener<Integer> l) {
    stateChangeListeners.add(l);
  }

  public static void fireStateChange(int state) {
    for (Listener<Integer> l : stateChangeListeners) {
      l.receive(state);
    }
  }

  protected static void processIncomingTTSEngineMessage(String text) {
    logger.info("From external TTS " + text);
    switch (text) {
    case "0":
      isTalking = true;
      break;
    case "1":
    case "2":
      isTalking = false;
      break;
    // case "2":
    // useMary = false;
    // isTalking = false;
    // doAction("", "Error while trying to reach maryserver, switched to
    // TTS",
    // true);
    // break;
    }
  }

  @SuppressWarnings("unchecked")
  protected static void processIncomingSoundListener(Object data) {
    ArrayList<Object> dblData = (ArrayList<Object>) data;
    double confidence = (float) dblData.get(4);
    if (confidence > 0.5) {

      if (Math.abs((float) dblData.get(2)) < 2.5) {

        float currentHeadYaw = (float) dblData.get(6);
        float currentHeadPitch = (float) dblData.get(7);

        ArrayList<String> names = new ArrayList<String>();
        names.add("HeadYaw");
        names.add("HeadPitch");
        ArrayList<Float> angles = new ArrayList<Float>();
        float toTurnYaw = currentHeadYaw + (float) dblData.get(2);
        float toTurnPitch = currentHeadPitch + (float) dblData.get(3);
        // System.out.println("turn " + toTurnYaw + " " + toTurnPitch);
        // System.out.println("turn " + toTurnYaw * toDegr + " " +
        // toTurnPitch
        // * toDegr);
        angles.add(toTurnYaw);
        angles.add(toTurnPitch);
        // float speed = localizationSpeed;
        // TODO how to handle messages from NAO???
        // doAutoMotion("HeadLocalization", names, angles, speed, true);
      }
    }
  }
  
  

  protected static void processNAOTTSMessage(final String state) {
    // logger.info("catch TTS " + state);
	switch (state) {
    case "started":
    case "Audio started playing.":
      isTalking = true;
      break;
    case "enqueued":
    case "Audio line opened.":
      break;
    case "thrown":
      break;
    case "stopped":
    case "Audio stopped playing.":
      break;
    case "done":
    case "Audio line closed.":
      isTalking = false;
      break;
    default:
      break;
    }
  }

  private NAOConnector() {

  }

  protected static final Logger logger = Logger.getLogger(NAOConnector.class);

  private static void waitForMaxTimeIsTalking(final boolean waitFor,
      final int during) {
    int count = during;
    while (count != 0) {
      sleep(250);
      count -= 250;
      if (isTalking == waitFor) {
        count = 0;
      }
    }
    // System.out.println("waitformax Talking " + count);
  }

  private static void waitForMaxTimeIsProcessing(final boolean waitFor,
      final int during) {
    int count = during;
    while (count != 0) {
      sleep(250);
      count -= 250;
      if (isProcessing == waitFor) {
        count = 0;
      }
    }
    // System.out.println("waitformax" + count);
  }

  private static boolean isProcessing = true;

  public static void initConnector() {

    if (System.getProperty(NAO_CONFIG_FILE_NAME_PROP) == null) {
      System.setProperty(NAO_CONFIG_FILE_NAME_PROP,
          "share/naoconnector.properties");
    }
    Utils.readGlobalProperties(NAO_CONFIG_FILE_NAME_PROP,
        necessaryNAOProperties);

    speechVolume = Utils.getIntProperty(SPEECH_VOLUME_PROP,
        "The volume of the speech");
    naoIPAddress = Utils.getProperty(NAO_IP_PROP, "IP address of the NAO");
    pitchShift = Utils.getFloatProperty(SPEECH_PITCH_PROP,
        "the pitch of the voice");
    speechSpeed = Utils.getIntProperty(SPEECH_SPEED_PROP,
        "the speed of the voice");
    useExtTTS = Utils.getBoolProperty(USE_EXTERNAL_TTS_PROP,
        "Indicator to use External TTS or not");
    extTTSClient = Utils.getProperty(EXT_TTS_BEHAVIOR_ONNAO_PROP,
        "Name of the TTSClient behavior on the NAO");
    extTTSHost = Utils.getProperty(EXT_TTS_HOST_PROP,
        "Name of the TTSServer host");

    naoName = Utils.getProperty(NAO_NAME_PROP, "Name of the NAO");
    autoConnect = Utils.getBoolProperty(NAO_AUTO_CONNECT,
        "Do you want connect to NAO at startup");

    useLocalization = Utils.getBoolProperty(USE_LOCALIZATION_PROP,
        "Do you want to use sound localization");

    localizationSensitivity = Utils.getFloatProperty(
        LOCALIZATION_SENSITIVITY_PROP, "the sensitivity of localization");

    localizationSpeed = Utils.getFloatProperty(LOCALIZATION_MOVE_SPEED_PROP,
        "the speed of movement of localization");

    runOnNAO = Utils.getBoolProperty(USE_ON_NAO, "Run on NAO");

  }
  //
  // public static void startConnector() {
  // if (autoConnect) {
  // connect();
  // }
  // }

  private static int speechVolume = 0;
  private static int speechSpeed = 100;
  private static int speechShape = 100;
  private static float pitchShift = 1.0F;
  private static float bodyStiffnesses = 1.0F;
  private static String currentLanguage = "";
  private static boolean autoConnect = false;
  private static boolean useExtTTS = false;
  private static String extTTSClient = "";
  private static String naoIPAddress = "127.0.0.1";
  private static String extTTSHost = "mary.dfki.de";
  private static String naoName = "NAO";
  private static boolean runOnNAO = false;

  private static float localizationSensitivity = 0.9f;
  private static boolean useLocalization = false;
  private static float localizationSpeed = 0.2f;

  public static float getLocalizationSensitivity() {
    return localizationSensitivity;
  }

  public static void setLocalizationSensitivity(float locSensitivity) {
    localizationSensitivity = locSensitivity;
    try {
      locateSound.call("setParameter", "Sensitivity", localizationSensitivity);
    } catch (CallError e) {
      e.printStackTrace();
    }
  }

  public static float getLocalizationSpeed() {
    return localizationSpeed;
  }

  public static void setLocalizationSpeed(float localizationSpeed) {
    NAOConnector.localizationSpeed = localizationSpeed;
  }

  public static boolean getUseSoundLocalization() {
    return useLocalization;
  }

  public static boolean getUseExternalTTS() {
    return useExtTTS;
  }

  protected static void setUseTTSValue(final boolean newState) {
    useExtTTS = newState;
  }

  public static void setUseSoundLocMode(boolean value) {
    useLocalization = value;
    try {
      if (useLocalization) {
        startLocalization();
      } else {
        stopLocalization();
      }
    } catch (Exception e) {
      logger.error("Error during start/stop localization " + e.getMessage());
    }
  }

  public static String getNaoName() {
    return naoName;
  }

  public static void setNaoName(String name) {
    naoName = name;
  }

  public static String getNaoIPAddress() {
    return naoIPAddress;
  }

  public static String getTTSAddress() {
    return extTTSHost;
  }

  public static void setTTSAddress(String address) {
    extTTSHost = address;
  }

  public static void setNaoIPAddress(final String newIpAddress) {
    naoIPAddress = newIpAddress;
  }

  public static boolean getAutoConnect() {
    return autoConnect;
  }

  public static boolean setAutoConnect() {
    return autoConnect;
  }

  public static int getSpeechSpeed() {
    return speechSpeed;
  }

  public static int getSpeechShape() {
    return speechShape;
  }

  public static void setSpeechShape(final int value) {
    synchronized (variablesLock) {
      speechShape = value;
    }
  }

  public static void setSpeechSpeed(final int value) {
    synchronized (variablesLock) {
      speechSpeed = value;
    }
  }

  public static void setCurrentLanguage(final String value) {
    currentLanguage = value;
  }

  public static String getCurrentLanguage() {
    return currentLanguage;
  }

  public static void setPitchShift(final float value) {
    synchronized (variablesLock) {
      pitchShift = value;
      if (isConnected()) {
        if (tts != null) {
          try {
            tts.call("setParameter", "pitchShift", pitchShift);
          } catch (CallError e) {
            logger.error("Error during tts-call " + e.getMessage());
          }
        }
      }
    }
  }

  public static float getPitchShift() {
    return pitchShift;
  }

  public static void setVolume(final int value) {
    synchronized (variablesLock) {
      speechVolume = value;
      if (isConnected()) {
        if (tts != null) {
          try {
            tts.call("setVolume", 1.0);
          } catch (CallError e) {
            logger.error("Error during tts-call " + e.getMessage());
          }
        }
        if (audioDevice != null) {
          try {
            audioDevice.call("setOutputVolume", value);
          } catch (CallError e) {
            logger.error("Error during tts-call " + e.getMessage());
          }
        }
      }
    }
  }

  public static int getVolume() {
    return speechVolume;
  }

  protected static void setBodyStiffness(final float value,
      final boolean waitAllStopped) {
    synchronized (variablesLock) {
      if (isConnected()) {
        if (motion != null) {
          if ((waitAllStopped) & (value < 1)) {
            // wait until all movements have stopped
            waitForMaxTimeIsProcessing(false, 3000);
          }
          synchronized (lockMotion) {
            try {
              motion.call("setStiffnesses", "Body", value);
            } catch (CallError e) {
              logger.error("Error during setting stiffness to "
                           + bodyStiffnesses + "  " + e.getMessage());
            }
          }
        }
        bodyStiffnesses = value;
      }
    }

  }

  protected static void setBodyStiffness(final float value) {
    setBodyStiffness(value, true);
  }

  public static float getBodyStiffness() {
    return bodyStiffnesses;
  }

  private static boolean testIfRightIPAddress(final String testAddress) {
    try {
      InetAddress inet = InetAddress.getByName(testAddress);

      return inet.isReachable(5000);

    } catch (IOException e) {
      logger.debug("Error during testIfRightIPAddress \n" + e.getStackTrace());
    }
    return false;
  }

  private static String tecsserver = "";
  private static int tecsPort = 0;

  public static boolean connect() {
	boolean connected=true;
    boolean presence = testIfRightIPAddress(naoIPAddress);

    logger.info(
        "presence: " + presence + " tcp://" + naoIPAddress + ":" + hostPort);
    if (presence) {
      try {
        session = new Session();

        logger.info("session started ");

        naoConnect = session.connect("tcp://" + naoIPAddress + ":" + hostPort);
        naoConnect.get();
        behaviorMngr = session.service("ALBehaviorManager");
        if (useExtTTS) {
          startBehaviorOnNAO(extTTSClient, false, 1);
        }
        tts = session.service("ALTextToSpeech");
        motion = session.service("ALMotion");
        memory = session.service("ALMemory");
        posture = session.service("ALRobotPosture");
        leds = session.service("ALLeds");

        // getVariablesFromNAO();
        //
        // Iterator<?> iter = behaviorsFromXML.entrySet().iterator();
        // while (iter.hasNext()) {
        // @SuppressWarnings("unchecked")
        // Map.Entry<String, Behavior> pairs = (Map.Entry<String,
        // Behavior>)
        // iter
        // .next();
        // Behavior item = pairs.getValue();
        // item.setMotionProxie(motion, lockMotion);
        // item.setTECSProxie(clientTECS);
        // }
        logger.info("ussExtTTS " + useExtTTS);

        if (!useExtTTS) {
          NAOCallback callbackTTS = new NAOCallback(ttsListener);
          Object subscriberTTS = memory
              .<AnyObject> call("subscriber", "ALTextToSpeech/Status").get();
          ((AnyObject) subscriberTTS).connect("signal::(m)",
              "onTTSChanged::(m)", callbackTTS);
        } else {
          NAOCallback callbackTTS = new NAOCallback(extTTSListener);
          String memoryLocation = "PAL/FinishedTalking";
          Object subscriberTTS = memory
              .<AnyObject> call("subscriber", memoryLocation).get();
          ((AnyObject) subscriberTTS).connect("signal::(m)", "onTTS::(m)",
              callbackTTS);
        }

        // if (locateSound != null) {
        NAOCallback callbackTouch = new NAOCallback(touchListener);
        Object subscriberTouch = memory
            .<AnyObject> call("subscriber", "TouchChanged").get();
        ((AnyObject) subscriberTouch).connect("signal::(m)", "onTouch::(m)",
            callbackTouch);
        // }
        logger.info("isConnected ");

        // checkStatus();
        isConnected();

        // do this last, since it "breaks on using a simulator"
        audioDevice = session.service("ALAudioDevice");
        locateSound = session.service("ALSoundLocalization");
        setVolume(speechVolume);
        
        if (useLocalization) {
          startLocalization();
          locateSound.call("setParameter", "Sensitivity",
              localizationSensitivity);
        }
        logger.info("Ready ");

      } catch (Exception e) {
        logger.error("Error during connecting to " + naoIPAddress + ":"
                     + hostPort + "  " + e.getMessage());
        connected=false;
      }
    } else
    	connected=false;
    //if command line is used to configure the tecserver location then use those settings
    //otherwise overwrite from memory (NAO) or config file
    if (!rise.connectors.nao.NAOConnector.TECSFromCommandLine){
    	
	    if (runOnNAO) {
	      try {
	        tecsserver = memory.<String> call("getData", "PAL/ServerIP").get();
	        String tecsserverPort = memory
	            .<String> call("getData", "PAL/ServerPort").get();
	        tecsPort = Integer.parseInt(tecsserverPort);
	      } catch (InterruptedException e) {
	        logger.error("Error during reading memory " + e.getMessage());
	      } catch (CallError e) {
	        logger.error("Error during reading memory " + e.getMessage());
	      }
	    } else {
	      tecsserver = System.getProperty(TECSSERVER_IP_PROP);
	      tecsPort = Integer.parseInt(System.getProperty(TECSSERVER_PORT_PROP));
	
	    }
    }
    logger.info("Using TECS ip " + tecsserver + ":" + tecsPort);

    
    
    /**generate special purpose detection/processing threads for the NAO**/
    //generate Noise Detection thread
    //detectNoise = new NAONoiseDetector(session);
    //detectNoise.start(); 
    
    
		// generate Voice Command Detector thread
		detectVoiceCommand = new VoiceCommandDetector(session);
		detectVoiceCommand.start();
		
		// generate Speech Recognition thread
		speechRecognition = new SpeechRecognition(session);
		speechRecognition.start();
		
		
    /*try {
    	aLife=new ALAutonomousLife(session);
		aLife.setState("solitary");
	} catch (Exception e) {e.printStackTrace();System.out.println("Error starting Alife"); aLife=null;}
    */
    gotoPosture(TOSTANDUP);
    
    monitorState();
    connectToTECS(riseConstants.NAOConnector);
    return connected;
  }

  private static void monitorState(){
	  Thread monitor= new Thread(){
		  boolean wasConnected=false;
		  int cantConnect=0;
		  @Override
	      public void run() {
			  while (isConnected()){
				  try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				  
				  //only do all of this if we are actually connected to NAOqi
				  //otherwise we have a problem
				  System.out.println("Connection status [NAO, TECS]:"+isConnected()+","+clientTECS.isConnected());
				  if (clientTECS.isConnected()){
					  if (wasConnected){
						  //do nothing, we are running
					  } else {
						  //give feedback to say we are ready
						  sayTextNoWait(READY);
						  if (aLife!=null){
							  try {
								aLife.setState("disabled");
							} catch (Exception e) {System.out.println("Can't stop aLife");}
						  }
					  }
					  
					  wasConnected=true;
					  cantConnect=1;
				  } else {
					  if (wasConnected){
						  sayTextNoWait(DISCONNECTED);
						  if (aLife!=null){
							  try {
								aLife.setState("solitary");
							} catch (Exception e) {System.out.println("Can't start aLife");}
						  }
					  } else
					  {	
						  if (cantConnect>30)
						  {
							  sayTextNoWait(CANTCONNECT);
							  
							  cantConnect=1;
						  } else if (cantConnect==0){
							  sayTextNoWait(CONNECTING);
						  }
					  }
					  cantConnect++;
					  wasConnected=false;
				  }	
			  }
		  }
	  };
	  monitor.start();
  }
  private static void startLocalization()
      throws CallError, InterruptedException, Exception {
    locateSound.call("unsubscribe", "NAOConnector");
    locateSound.call("subscribe", "NAOConnector");
    if (locateSound != null) {
      NAOCallback callbackSound = new NAOCallback(soundListener);
      Object subscriberSound = memory
          .<AnyObject> call("subscriber", "ALSoundLocalization/SoundLocated")
          .get();
      ((AnyObject) subscriberSound).connect("signal::(m)",
          "onSoundLocated::(m)", callbackSound);
    }
  }

  private static void stopLocalization()
      throws CallError, InterruptedException, Exception {
    locateSound.call("unsubscribe", "NAOConnector");
  }

  private static Object variablesLock = new Object();

  public static void disConnect() {
	  //detectNoise.stopListening();
	  speechRecognition.stopListening();
	  detectVoiceCommand.stopDetecting();
	  if (clientTECS != null) {
      clientTECS.disconnect();
    }

    if (useExtTTS) {
      stopBehaviorOnNAO(extTTSClient);
    }

    // only start this if a real NAO is connected
    if (!naoIPAddress.equals("127.0.0.1")) {
      if (isConnected()) {
        keepProcessing = false;
        incomingLLEActions.clear();

        gotoPosture(TOCROUCHED);
        // startBehaviorOnNAO(TOCROUCHED, false, 2);
        setBodyStiffness(0.0F);

        if (session != null) {
          try {
            locateSound.call("unsubscribe", "NAOConnector");
            session.close();
          } catch (Exception e) {
            logger.error("Error during connecting to " + naoIPAddress + ":"
                         + hostPort + "  " + e.getMessage());
          }
        }
      }
    }
    if (connectionState) {
      connectionState = false;
      fireStateChange(-1);
    }
  }

  private static void sayTextNoWait(final String text) {
    if (isConnected()) {
      String stripped = text.toLowerCase();
      logger.info("sayTextNoWait (" + stripped + ") " + useExtTTS);
      if (stripped.length() > 1) {
        if (useExtTTS) {
          if (memory != null) {
            try {
              // e-grave
              stripped = stripped.replace("\u00E8", "e");
              stripped = stripped.replace("\u00E9", "e");
              stripped = stripped.replace("\u00EA", "e");
              stripped = stripped.replace("é", "e");
              stripped = stripped.replace("ê", "e");
              stripped = stripped.replace("è", "e");
              // a-grave
              stripped = stripped.replace("\u00E0", "a");
              stripped = stripped.replace("\u00E1", "a");
              stripped = stripped.replace("\u00E2", "a");
              stripped = stripped.replace("â", "a");
              stripped = stripped.replace("à", "a");
              stripped = stripped.replace("å", "a");
              // i-grave
              stripped = stripped.replace("\u00EC", "i");
              stripped = stripped.replace("\u00ED", "i");
              stripped = stripped.replace("\u00EE", "i");
              stripped = stripped.replace("ì", "i");
              stripped = stripped.replace("î", "i");

              if (extTTSClient.contains("espeak")) {
                stripped = mimicProdisity(stripped);
              }

              // wait since otherwise two texts are spooken
              logger.info("test is tts talking");
              waitForMaxTimeIsTalking(false, 5000);
              logger.info("write to memory on NAO");
              // set this to true (is also done when the tts start,
              // but that might take to long and than
              // texts are spoken thru each other.
              isTalking = true;
              memory.call("raiseEvent", "PAL/TextToSpeak", stripped);
            } catch (CallError e) {
              e.printStackTrace();
            }
          }
        } else {
          if (tts != null) {
            // I know very crazy hack, but this sounded best for dutch ;-)
            stripped = stripped.replace("1.5", "ander halve");
            stripped = stripped.replace("een.5", "ander halve");
            stripped = stripped.replace("een,5", "ander halve");
            stripped = stripped.replace("\u00E9\u00E9n.5", "ander halve");
            stripped = stripped.replace("\u00E9\u00E9n,5", "ander halve");
            stripped = stripped.replace("een tot ", "\u00E9\u00E9n tot");
            stripped = stripped.replace("2.5", "twee en een halve");
            stripped = stripped.replace("twee,5", "twee en een halve");
            stripped = stripped.replace("spuiten", "spui ten");

            stripped = mimicProdisity(stripped);
            try {
              // logger.info("send to TTS " + stripped);
              tts.call("say", "\\RSPD=" + speechSpeed + "\\ " + " \\VCT="
                              + speechShape + "\\ " + stripped);
            } catch (CallError e) {
              logger.error("Error during tts-call " + e.getMessage());
            }
          }
        }
      }
    }
  }

  private static String mimicProdisity(String stripped) {
    // mimic the prodisity when using the NAO-tts
    String pause = "\\pau=100\\";
    if (extTTSClient.contains("espeak")) {
      pause = " ";
    }
    if (stripped.contains("\u00A7")) {
      stripped = stripped.replace("\u00A7", pause);
      // replacing these charactres might result in a '.' or '?'
      // after the pause which result in explicit speaking that
      stripped = stripped.replace("^.", "." + pause);
      stripped = stripped.replace("^?", "?" + pause);
      stripped = stripped.replace("^ .", "." + pause);
      stripped = stripped.replace("^ ?", "?" + pause);
      stripped = stripped.replace("^", pause);
      stripped = stripped.replace(" .", "");
      stripped = stripped.replace(" ?", "");
    }
    return stripped;
  }

  public static void sayText(final String text, final int cmdId) {
    // prevent sending !, : and that sort of "text"
    // if (text.trim().length() == 1) {
    // Pattern pattern = Pattern.compile("[a-z][A-Z][0-9]");
    // if (!pattern.matcher(text).matches()) {
    // return;
    // }
    // }
    if (isConnected()) {
      // String stripped = text.replace("\u00A7", ""); // paragraph
      // symbol??
      // stripped = stripped.replace("^", "");
      // if (wait) {
      // setPauseAutonomousValue(true);
      // }
      sayTextNoWait(text);
      // wait for the TTS to finish
      waitForMaxTimeIsTalking(false, 5000);
      logger.info("Finished sayText " + cmdId + "  send");
      sendToServer(new LowLevelNaoCommand(cmdId, "finished;"));
    }
  }

  public static boolean isConnected() {
    if (session != null) {
      if (session.isConnected()) {
        if (!connectionState) {
          connectionState = true;
          fireStateChange(0);
        }
        return true;
      }
    }
    if (connectionState) {
      connectionState = false;
      fireStateChange(-1);
    }
    return false;
  }

  public static void setLanguage(final String newLanguage) {
    if (isConnected()) {
      try {
        String language = newLanguage;
        switch (newLanguage.toLowerCase()) {
        case "english":
        case "eng":
          language = "English";
          break;
        case "dutch":
        case "dut":
          language = "Dutch";
          break;
        case "german":
        case "ger":
          language = "German";
          break;
        case "italian":
        case "ita":
          language = "Italian";
          break;
        default:
          break;
        }
        if (useExtTTS) {
          if (memory != null) {
            try {
              String params = "language=" + language + ";host=" + extTTSHost;
              memory.call("raiseEvent", "PAL/ParametersToSpeak", params);
            } catch (CallError e) {
              e.printStackTrace();
            }
          }
        } else {
          if (tts != null) {
            tts.call("setLanguage", language);
          }
        }
        setCurrentLanguage(language);
      } catch (CallError e) {
        logger.error("Error during tts-call " + e.getMessage());
      }
    }
  }

  public static ArrayList<String> getBehaviors() {
    // ArrayList<String> names = getBehaviorsFromXML();
    ArrayList<String> behaviorsOnNAO = getBehaviorsFromNAO();
    // names.addAll(names2);
    Collections.sort(behaviorsOnNAO);
    return behaviorsOnNAO;
  }

  private static ArrayList<String> getBehaviorsFromNAO() {
    ArrayList<String> names = new ArrayList<String>();
    if (isConnected()) {
      if (behaviorMngr != null) {
        try {
          Object myBehaviors = (Object) behaviorMngr
              .<AnyObject> call("getInstalledBehaviors").get();
          @SuppressWarnings("unchecked")
          ArrayList<Object> myBehList = (ArrayList<Object>) myBehaviors;
          for (Object object : myBehList) {
            names.add(object.toString());
            // behaviorsOnNAO.add(object.toString());
          }
          return names;
        } catch (CallError | InterruptedException e) {
          System.out
              .println("Error during getBehaviors-call " + e.getMessage());
        }
      }
    }
    return names;
  }

  private static String getPosture() {
    if (isConnected()) {
      if (posture != null) {
        try {
          Object currentPosture = posture.<AnyObject> call("getPosture").get();
          String result = (String) currentPosture;
          return result;
        } catch (CallError | InterruptedException e) {
          logger.error("Error during getPosture-call " + e.getMessage());
        }
      }
    }
    return "None";
  }

  private static void gotoPosture(final String name) {
    if (isConnected()) {
      if (posture != null) {
        try {
          // take it easy on the speed
          // and wait to finish
          float speed = 0.6F;
          posture.<AnyObject> call("goToPosture", name, speed).get();
          //XXX
          sendToServer(new LowLevelNaoCommand(654, "finished;")); 
        } catch (CallError e) {
          logger.error("Error during posture-call " + e.getMessage());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void setEyeColor(final String color) {
    if (isConnected()) {
      if (leds != null) {
        try {
          String name = "white";
          String eyecolor = "255;255;255";
          switch (color.toLowerCase()) {
          case "red":
          case "rood":
            name = "red";
            eyecolor = "255;0;0";
            break;
          case "blue":
          case "blauw":
            name = "blue";
            eyecolor = "0;0;255";
            break;
          case "green":
          case "groen":
            name = "green";
            eyecolor = "0;255;0";
            break;
          case "yellow":
          case "geel":
            name = "yellow";
            eyecolor = "255;255;0";
            break;
          case "cyan":
          case "cyaan":
            name = "cyan";
            eyecolor = "0;255;255";
            break;
          case "magenta":
            name = "magenta";
            eyecolor = "255;0;255";
            break;
          }
          leds.call("fadeRGB", "FaceLeds", name, 0.5);
          LowLevelNaoCommand llc = new LowLevelNaoCommand(823,
              "eyecolor;" + eyecolor);
          sendToServer(llc);

        } catch (CallError e) {
          logger.error("Error during leds " + e.getMessage());
        }
      }
    }

  }

  protected static void doLeds(final int setting, final float BlinkDuration) {
    if (isConnected()) {
      if (leds != null) {
        try {
          leds.call("fadeRGB", "FaceLeds", setting, BlinkDuration);
        } catch (CallError e) {
          logger.error("Error during leds " + e.getMessage());
        }
      }
    }
  }

  protected static void doBlink(final int on, final int off,
      final float rDuration) {
    if (isConnected()) {
      if (leds != null) {
        try {
          int index = 0;
          leds.call("fadeRGB", "FaceLed" + index++, off, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, off, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, on, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, off, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, off, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, off, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, on, rDuration);
          leds.call("fadeRGB", "FaceLed" + index++, off, rDuration);
          sleep(100);
          leds.call("fadeRGB", "FaceLeds", on, rDuration);
        } catch (CallError e) {
          logger.error("Error during leds " + e.getMessage());
        }
      }
    }
  }

  protected static void stopBehaviorOnNAO(final String name) {
    if (behaviorMngr != null) {
      try {
        behaviorMngr.call("stopBehavior", name);
        // behaviorMngr.<AnyObject> call(name);
      } catch (CallError e) {
        logger.error("Error during runBehavior-call " + e.getMessage());
      }
    }
  }

  private static void startBehaviorOnNAO(final String name, final boolean wait,
      final int id) {
    if (behaviorMngr != null) {
      // setPauseAutonomousValue(true);
      setBodyStiffness(1.0F);
      try {
        behaviorMngr.call("runBehavior", name);
        isMoving=true;
        // wait for behavior to start
        sleep(1500);
        if (wait) {
          Thread runBehaviorandWait = new Thread() {
            @Override
            public void run() {
              try {
                boolean isRunning = true;
                while (isRunning) {
                  sleep(500);
                  Object isRunningObj = behaviorMngr
                      .<AnyObject> call("isBehaviorRunning", name).get();
                  isRunning = (boolean) isRunningObj;
                }
                isMoving=false;
                if (wait) {
                  sendToServer(new LowLevelNaoCommand(id, "finished;"));
                }
                
                // System.out.println("Gestopt");
              } catch (CallError e) {
                logger.error("Error during runBehavior-call " + e.getMessage());
              } catch (InterruptedException e) {
                logger.error("Error during waiting to stop " + e.getMessage());
              }
            }
          };
          runBehaviorandWait.start();
        }
      } catch (CallError e) {
        logger.error("Error during runBehavior-call " + e.getMessage());
      }
    }
  }

  public static void test() {

    try {
      // System.out.println("start move");
      float[] walkParameters = new float[] { 0.10F, 0.0F, 0.0F };
      String walkFunction = "moveTo";

      motion.<AnyObject> call(walkFunction, walkParameters[0],
          walkParameters[1], walkParameters[2]).get();
      // System.out.println("end move");
    } catch (CallError | InterruptedException e) {
      logger.error("Error during runBehavior-call " + e.getMessage());
    }

  }

  private static void sleep(final long sleepTime) {
    try {
      Thread.sleep(sleepTime);
    } catch (InterruptedException v) {
      logger.error(v);
    }
  }

  public static void stopTalking() {
    if (tts != null) {
      try {
        // logger.info("send to TTS " + stripped);
        tts.call("stopAll");
      } catch (CallError e) {
        logger.error("Error during tts-call " + e.getMessage());
      }
    }

  }

  /***************
   * TECS client parts
   */
  private static TECSClient clientTECS = null;

  private static EventHandler<LowLevelNaoCommand> palLLCmdEventHandler = new EventHandler<LowLevelNaoCommand>() {

    public void handleEvent(LowLevelNaoCommand event) {
      receive(event);
    }
  };

  private static EventHandler<LowLevelExecuteCommand> palLLEventHandler = new EventHandler<LowLevelExecuteCommand>() {

    public void handleEvent(LowLevelExecuteCommand event) {
      receive(event);
    }
  };

  private static EventHandler<SystemInfo> palSysInfoEventHandler = new EventHandler<SystemInfo>() {

    public void handleEvent(SystemInfo event) {
      receive(event);
    }
  };

  private static boolean keepListening = true;

  private static void receive(SystemInfo event) {
    String command = event.getContent();

    if (command.toLowerCase().contains("end_session")) {
      keepListening = false;
      gotoPosture("Crouch");
      setBodyStiffness(0.0f);
    }

  }

  @SuppressWarnings("rawtypes")
  public static void sendToServer(final TBase message) {
    if (clientTECS != null) {
      clientTECS.send(message);
    }
  }

  public static void connectToTECS(final String clientName) {
	  clientTECS = new TECSClient(tecsserver, clientName, tecsPort);
      if (clientTECS != null) {
        clientTECS.subscribe(riseConstants.LowLevelNaoCommandMsg, palLLCmdEventHandler);
        clientTECS.subscribe(riseConstants.LowLevelExecuteCommandMsg, palLLEventHandler);
        clientTECS.subscribe(riseConstants.SystemInfoMsg, palSysInfoEventHandler);
		clientTECS.subscribe(riseConstants.GetSpeechMsg, SpeechRecognition.getSpeechEventHandler);
		clientTECS.subscribe(riseConstants.GetVoiceCommandMsg,
							VoiceCommandDetector.getVoiceCommandEventHandler);
        clientTECS.startListening();
      }
  }

  /*********************/

  private static boolean keepProcessingAction = true;

  private static Object incomingCommandsLock = new Object();
  private static Object incomingActionsLock = new Object();
  // private static LinkedList<LowLevelNaoCommand> incomingLLCmdActions = new
  // LinkedList<LowLevelNaoCommand>();
  @SuppressWarnings("rawtypes")
  private static LinkedList<TBase> incomingLLCActions = new LinkedList<TBase>();
  @SuppressWarnings("rawtypes")
  private static LinkedList<TBase> incomingWaitActions = new LinkedList<TBase>();
  @SuppressWarnings("rawtypes")
  private static LinkedList<TBase> incomingLLEActions = new LinkedList<TBase>();

  // start separate thread to monitor and handle incoming messages
  private static void startProcessingThread() {
    Thread incommingExecutor = new Thread() {
      @Override
      public void run() {
        executeProcessActionsThread();
      }
    };
    incommingExecutor.start();
  }

  // start separate thread to monitor and handle incoming messages
  private static void startProcessingCommandsThread() {
    Thread incommingExecutor = new Thread() {
      @Override
      public void run() {
        executeProcessCommandsThread();
      }
    };
    incommingExecutor.start();
  }

  @SuppressWarnings("rawtypes")
  private static void executeProcessActionsThread() {

    while (keepProcessingAction) {
      TBase input = null;
      synchronized (incomingWaitActions) {
        if (incomingWaitActions.size() > 0) {
          input = incomingWaitActions.getFirst();
          incomingWaitActions.removeFirst();
        }
      }
      if (input == null) {
        synchronized (incomingActionsLock) {
          if (!incomingLLEActions.isEmpty()) {
            logger.debug("executeProcessActionsThread queuesize:"
                         + incomingLLEActions.size());
            input = incomingLLEActions.getFirst();
            incomingLLEActions.removeFirst();
            // logger.debug(" execute: " + ((LowLevelNaoCommand)input).command);
          }
        }
      }
      if (input != null) {
        if (input instanceof LowLevelExecuteCommand) {
          processIncomingAction((LowLevelExecuteCommand) input);
        }
        if (input instanceof LowLevelNaoCommand) {
          processIncomingAction((LowLevelNaoCommand) input);
        }
      } else {
        // don't stall cpu
        sleep(10);
      }
    }
    isProcessingIncomming = false;
  }

  @SuppressWarnings("rawtypes")
  private static void executeProcessCommandsThread() {

    while (keepProcessingAction) {
      TBase input = null;
      synchronized (incomingCommandsLock) {
        if (!incomingLLCActions.isEmpty()) {
          logger.debug("executeProcessActionsThread queuesize:"
                       + incomingLLEActions.size());
          input = incomingLLCActions.getFirst();
          incomingLLCActions.removeFirst();
          // logger.debug(" execute: " + ((LowLevelNaoCommand)input).command);
        }
      }

      if (input != null) {
        if (input instanceof LowLevelNaoCommand) {
          processIncomingAction((LowLevelNaoCommand) input);
        }
      } else {
        // don't stall cpu
        sleep(10);
      }
    }
    isProcessingCmdsIncomming = false;
  }

  private static boolean isProcessingIncomming = false;
  private static boolean isProcessingCmdsIncomming = false;

  private static void receive(LowLevelExecuteCommand event) {
    logger.info("LLExecCommand " + event.id + "  '" + event.getTextToSpeak()
                + "'" + event.waitToConpleet);
    if (keepListening) {
      if (event.waitToConpleet) {
        synchronized (incomingWaitActions) {
          incomingWaitActions.add(event);
          logger.info("number waitaction " + incomingWaitActions.size() + "  '"
                      + event.getTextToSpeak() + "'");
        }
      } else {
        synchronized (incomingActionsLock) {
          // if incoming is a wait-event clear all previous
          // if the queue is to long clear it
          if (incomingLLEActions.size() > 20) {
            logger.info("clear " + incomingLLEActions.size() + " "
                        + event.waitToConpleet);
            incomingLLEActions.clear();
          }
          // if the queue is large then do not add commands to move head or arms
          if (incomingLLEActions.size() > 15) {
            boolean skip = false;
            int count = 0;
            while (!skip && count < event.getJoints().size()) {
              String joint = event.getJoints().get(count);
              skip = (joint.contains("Head") || joint.contains("Shoulder"));
              count++;
            }
            if (skip) {
              logger.info("Do not use " + event.getId());
            } else {
              incomingLLEActions.add(event);
            }
          } else {
            incomingLLEActions.add(event);
          }
        }
      }
      // take care this processing thread is only stared once
      if (!isProcessingIncomming) {
        isProcessingIncomming = true;
        startProcessingThread();
      }
    } else {
      logger.info("LowLevelExecuteCommand Stopped listening");
    }
  }

  private static String lastContent = "";

  private static void receive(LowLevelNaoCommand event) {
    if (keepListening) {

      synchronized (incomingCommandsLock) {
        String command = event.getCommand();
        logger.info("LLNAOCommand " + command);
        // always handle blink commands
        if (command.startsWith("blink") || command.contains("posture")
            || command.startsWith("setspeech")) {
          incomingLLCActions.add(event);
        } else {
          // test if this is not handled just before
          if (!event.getCommand().equals(lastContent)) {
            // store first incoming command
            if (lastContent == "") {
              lastContent = event.getCommand();
              incomingLLCActions.add(event);
            }
            // test if this is not handled just before
            if (!event.getCommand().equals(lastContent)) {
              incomingLLCActions.add(event);
              lastContent = event.getCommand();
            }
          }
        }
      }
      // take care this processing thread is only stared once
      if (!isProcessingCmdsIncomming) {
        isProcessingCmdsIncomming = true;
        startProcessingCommandsThread();
      }
    } else {
      logger.info("LowLevelNaoCommand Stopped listening");
    }
  }

  private static boolean keepProcessing = true;

  private static void processIncomingAction(
      final LowLevelExecuteCommand event) {

    if (keepProcessing) {
      if (event.getAngles().size() == 0) {
        logger.info("say no moves, so wait");
        sayText(event.getTextToSpeak(), event.getId());
      } else {
        logger.info("say with moves, speak no wait");
        sayTextNoWait(event.getTextToSpeak());
        executeJointsChanging(event.getId(), event.getJoints(),
            event.getAngles(), event.getTimes(), event.waitToConpleet);
      }
    }
  }

  private static void processIncomingAction(final LowLevelNaoCommand event) {
	  //When the event signals a build in behavior, the event looks like:
	  /*LowLevelNaoCommand action = new LowLevelNaoCommand(id, "startbehavior;" + id + ";" + behaviorName + ";" + textToSpeak + ";"+ wait);
      */
    String cmd = event.getCommand();
    // System.out.println("NAOConnector.receive LowLevelNaoCommand " + cmd);
    String[] parts = cmd.split(";");
    switch (parts[0].toLowerCase()) {
    case "disconnect":
    	disConnect();
    case "stopbehavior":
      stopBehaviorOnNAO(parts[1]);
      break;
    case "startbehavior":
      boolean toWait = parts[4].equalsIgnoreCase("true");
      sayTextNoWait(parts[3]);
      startBehaviorOnNAO(parts[2], toWait, Integer.parseInt(parts[1]));
      break;
    case "executeautobehavior":
      break;
    case "gotoposture":
    	if (parts[1].equals(TOSTANDUP))
       	 	setBodyStiffness(1.0F);
    	gotoPosture(parts[1]);
    	if (parts[1].equals(TOCROUCHED))
    		setBodyStiffness(0.0F);
     
      break;
    case "getposture":
      String currentPosture = getPosture();
      long currentTime = System.currentTimeMillis();
      if (currentTime - postureLastSend >= 1000) {
        postureLastSend = currentTime;
        logger.info("Send posture: " + currentPosture);
        sendToServer(new LowLevelNaoCommand(LLNAOCmd++,
            "actualposture;" + currentPosture));
      }
      break;
    case "blink":
      doBlink(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
          Float.parseFloat(parts[3]));
      break;
    case "setspeechpitch":
      setPitchShift(Float.parseFloat(parts[1]));
      break;
    case "setspeechspeed":
      setSpeechSpeed(Integer.parseInt(parts[1]));
      break;
    case "setspeechshape":
      setSpeechShape(Integer.parseInt(parts[1]));
      break;
    case "setTTSEngine":
      setTTSEngine(cmd);
      break;
    }
  }

  private static void setTTSEngine(final String cmd) {
    String[] parts = cmd.split(";");

  }

  protected static int LLNAOCmd = 1;
  private static long postureLastSend = 0;

  private static void executeJointsChanging(final int id,
      final List<String> jointNames, final List<List<Double>> angles,
      final List<List<Double>> times, boolean wait) {
    // final Double speed
    if (motion != null) {
      try {
        // System.out.println("start angle ");
        synchronized (lockMotion) {
          Object namesObj = null;
          Object anglesObj = null;
          Object timesObj = null;
          String callFunction = "";

          // currently not implemented!!
          // if (times.size() == 0) {
          // callFunction = "angleInterpolationWithSpeed";
          // timesObj = speed;
          // if (jointNames.size() == 1) {
          // namesObj = jointNames.get(0);
          // anglesObj = angles.get(0);
          // } else {
          // namesObj = jointNames;
          // anglesObj = angles;
          // }
          // } else {
          callFunction = "angleInterpolation";
          if (jointNames.size() == 1) {
            namesObj = jointNames.get(0);
            anglesObj = angles.get(0);
            timesObj = times.get(0);
          } else {
            namesObj = jointNames;
            anglesObj = angles;
            timesObj = times;
          }
          // }
          // }
          if (times.size() > 0) {
        	  isMoving=true;
            // System.out.println("Start " + jointNames.size());
            // for (int i = 0; i < times.get(0).size(); i++) {
            // System.out.print("; " + times.get(0).get(i));
            // }
            // System.out.println(";");
            // if (times.get(0).get(0) > 2.0) {
            // System.out.println(";");
            // }
            boolean isAbsolute = true;
            // if (wait) {
            motion.<Object> call(callFunction, namesObj, anglesObj, timesObj,
                isAbsolute).get();
            // } else {
            // motion.call(callFunction, namesObj, anglesObj, timesObj,
            // isAbsolute);
            // }
          } else {
            // if (wait) {
            motion.<Object> call(callFunction, namesObj, anglesObj, timesObj)
                .get();
            // } else {
            // motion.call(callFunction, namesObj, anglesObj, timesObj);
            // }
          }
          logger.info("Finished " + id + "  send:" + wait);
          isMoving=false;
          if (wait) {
            // check if no talking is still active
            waitForMaxTimeIsTalking(false, 10000);
            sendToServer(new LowLevelNaoCommand(id, "finished;"));
          }
          
        }
      } catch (CallError | InterruptedException e) {
        logger.error("Error during runBehavior-call " + e.getMessage());
      }
    }
  }

  private static String[] necessaryProperties = { TECSSERVER_IP_PROP,
      TECSSERVER_PORT_PROP, NAO_IP_PROP };

  public static void main(final String[] args) {
	  rise.connectors.nao.NAOConnector.TECSFromCommandLine=false;
	  if (args.length>1){
		  if (args.length>3)
			  //assume tecsip tecsport naoip naoport
			  startUp(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
		  else
			  //assume naoip  naoport (and this default tecs connectivity)
			  startUp(args[0], Integer.parseInt(args[1]));
	    } else
	    	startUp();
	  
  }
  public static boolean startUp(String TECSERVER, Integer TECSPORT, String IP, int port){
	  Utils.loadEnvironment(necessaryProperties);

	    rise.connectors.nao.NAOConnector.initConnector();
	    
    	rise.connectors.nao.NAOConnector.naoIPAddress=IP;
    	rise.connectors.nao.NAOConnector.hostPort=port;
    	rise.connectors.nao.NAOConnector.tecsserver=TECSERVER;
    	rise.connectors.nao.NAOConnector.tecsPort=TECSPORT;
    	rise.connectors.nao.NAOConnector.TECSFromCommandLine=true;
    	
	    return connect();
  }
  public static boolean startUp(String IP, int port){
	  Utils.loadEnvironment(necessaryProperties);

	    rise.connectors.nao.NAOConnector.initConnector();
	    
    	rise.connectors.nao.NAOConnector.naoIPAddress=IP;
    	rise.connectors.nao.NAOConnector.hostPort=port;
	    return connect();
  }
  public static boolean startUp(){
	  Utils.loadEnvironment(necessaryProperties);

	    rise.connectors.nao.NAOConnector.initConnector();
	    
    	 return connect();
  }
}