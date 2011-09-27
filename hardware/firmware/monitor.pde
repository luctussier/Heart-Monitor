#include <SdFat.h>
#include <Wire.h>
#include <RTClib.h>
// SD and RTClib come from github.com/adafruit/

#define led 9
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

// The objects to talk to the SD card
Sd2Card card;
SdVolume volume;
SdFile root;
SdFile file;
DateTime now;
uint32_t syncTime = 0; // time of last sync()

void setup() {
	Serial.begin(57600);
	Wire.begin();
	RTC.begin();

	pinMode(led, OUTPUT);
	pinMode(beatIn, INPUT);
	pinMode(redLED, OUTPUT);
	pinMode(greenLED, OUTPUT);
 
	digitalWrite(led, state);
	
	card.init();
	volume.init(card);
	root.openRoot(volume);
	
	now = RTC.now();
	char name[] = "yyyyMMdd.log";
	name[0] = now.year()/1000 + '0';
	name[1] = (now.year()%1000)/100 + '0';
	name[2] = (now.year()%100)/10 + '0';
	name[3] = now.year()%10 + '0';
	name[4] = now.month()/10 + '0';
	name[5] = now.month()%10 + '0';
	name[6] = now.day()/10 + '0';
	name[7] = now.day()%10 + '0';
	file.open(root, name, O_CREAT | O_WRITE | O_APPEND);

	file.print("----");
	file.print(now.hour(), DEC);
	file.print(":");
	file.print(now.minute(), DEC);
	file.print(":");
	file.println(now.second(), DEC);
	file.sync(); 

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
		file.println(ms, DEC);
		if(debug){
			// Serial.print("beat: ");
			Serial.println(ms);
		}
	}

	if(state && millis()>lightOff){
		state = LOW;
		digitalWrite(redLED, state);
	}

	if(Serial.available() > 0){
		debug = (Serial.read()=='s'); // s=start; any other value will disactivate it
	}

	// only sync when data is ready and the syncTime delay has elapsed
	if (!dataReady || (millis() - syncTime) < SYNC_INTERVAL) return;
	syncTime = millis();

	// blink LED to show we are syncing data to the card & updating FAT!
	digitalWrite(greenLED, HIGH);
	file.sync(); // sync requires 2048 bytes of I/O to SD card
	digitalWrite(greenLED, LOW);
}
