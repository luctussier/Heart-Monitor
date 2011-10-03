#include <SD.h>
#include <Wire.h>
#include <RTClib.h>
// RTC from github.com/adafruit/RTClib

#define chipSelect 10 // CS - pin 10
#define beatIn 5 // the digital pins that connect to the LEDs
// red=reading, calculating values; green=sync file write to chip
#define redLED 3
#define greenLED 4
#define SYNC_INTERVAL 5000 // mills between calls to sync()

boolean debug = false;
boolean state = LOW;
boolean dataReady = false;
long lightOff = 0;
uint32_t ms; // uint32_t == unsigned long
RTC_DS1307 RTC;
char buffer[0x32];
int readChar;
byte inputPos;
boolean eol = false;

// The objects to talk to the SD card
Sd2Card card;
SdVolume volume;
SdFile root;
SdFile logFile;
char logName[] = "yyyyMMdd.log";
DateTime now;
uint32_t syncTime = 0; // time of last sync/flush()

void setup() {
	Serial.begin(57600);
	Wire.begin();
	RTC.begin();
	pinMode(beatIn, INPUT);
	pinMode(redLED, OUTPUT);
	pinMode(greenLED, OUTPUT);

	pinMode(chipSelect, OUTPUT);
	if (!SD.begin(chipSelect)) { // CS - pin 10
		Serial.println("initialization failed!");
		return;
	}
	card.init();
	volume.init(card);
	root.openRoot(volume);
	
	now = RTC.now();
	logName[0] = now.year()/1000 + '0';
	logName[1] = (now.year()%1000)/100 + '0';
	logName[2] = (now.year()%100)/10 + '0';
	logName[3] = now.year()%10 + '0';
	logName[4] = now.month()/10 + '0';
	logName[5] = now.month()%10 + '0';
	logName[6] = now.day()/10 + '0';
	logName[7] = now.day()%10 + '0';
	logFile.open(root, logName, O_CREAT | O_WRITE | O_APPEND);

	logFile.print("----");
	logFile.print(now.hour(), DEC);
	logFile.print(":");
	logFile.print(now.minute(), DEC);
	logFile.print(":");
	logFile.println(now.second(), DEC);
	logFile.sync(); 

	// leave green light on while waiting for input
	// showing device is on and waiting
	digitalWrite(greenLED, HIGH);
}

void loop(){
	if(!state && digitalRead(beatIn)){
		dataReady = true;
		state = HIGH;
		ms = millis();
		lightOff = ms + 100;
		digitalWrite(redLED, state); // show that a beat was detected
		logFile.println(ms, DEC);
		if(debug){
			Serial.print("beat: ");
			Serial.println(ms);
		}
	}

	if(state && millis()>lightOff){
		state = LOW;
		digitalWrite(redLED, state);
	}

	// read serial data into 32 char buffer
	while(Serial.available() > 0){
		readChar = Serial.read();
		eol = (readChar=='\n');
		buffer[inputPos++] = eol ? 0 : readChar;
	}

	// data recovered; parse command and execute
	if(eol){
		File dir;
		File dataFile;
		switch(buffer[0]){
		case 'd':
			debug = !debug; // s=start start debug
			Serial.print("debug mode ");
			Serial.println(debug?"on":"off");
			break;
		case 'l':
			logFile.close(); // doc says only one can be open at a time
			root.ls(LS_SIZE);
			logFile.open(root, logName, O_CREAT | O_WRITE | O_APPEND);
			break;
		case 'o':
			logFile.close(); // doc says only one can be open at a time
			dataFile = SD.open(&buffer[2]);
			if (dataFile) { // if the file is available read it
				while (dataFile.available()) {
					Serial.write(dataFile.read());
				}
				dataFile.close();
			} else { // if the file isn't open; out message
				Serial.print("couldn't open ");
				Serial.println( &buffer[2]);
			}
			logFile.open(root, logName, O_CREAT | O_WRITE | O_APPEND);
			break;
		case 'r':
			logFile.close(); // doc says only one can be open at a time
			if(SD.exists(&buffer[2])){
				Serial.print(SD.remove(&buffer[2])?"successfully":"unsuccessfully");
				Serial.print(" removed ");
				Serial.println(&buffer[2]);
			}else{
				Serial.print("unable to find ");
				Serial.println(&buffer[2]);
			}
			logFile.open(root, logName, O_CREAT | O_WRITE | O_APPEND);
			break;
		case 't':
			Serial.print("time ");
			{
				now = RTC.now();
				Serial.print(now.hour(), DEC);
				Serial.print(":");
				Serial.print(now.minute(), DEC);
				Serial.print(":");
				Serial.print(now.second(), DEC);
			}
			Serial.println();
			break;
		default:
			Serial.println("command unknown");
		}
		inputPos = eol = false; // reset counters
	}

	// only sync when data is ready and the syncTime delay has elapsed
	if (!dataReady || (millis() - syncTime) < SYNC_INTERVAL) return;
	syncTime = millis();

	// blink LED to show we are syncing data to the card & updating FAT!
	digitalWrite(greenLED, HIGH);
	logFile.sync(); // sync requires 2048 bytes of I/O to SD card
	digitalWrite(greenLED, LOW);
}

