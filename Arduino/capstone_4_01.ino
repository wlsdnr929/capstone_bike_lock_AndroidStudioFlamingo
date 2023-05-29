#include "ESP8266.h"
#include <SoftwareSerial.h>

//#define SSID        "Won"
//#define SSID        "07302"  
#define SSID          "how you like that~"
//#define SSID        "Mingu"

//#define PASSWORD    "0730207302" 
#define PASSWORD      "fire eggs"
//#define PASSWORD    "25801111" 
//#define PASSWORD    "rnjsalsrn1" 

#define IN1_A  6   
#define IN2_A  5   

byte speedDC = 255;

SoftwareSerial mySerial(10, 9); /* TX:D10, RX:D9 */
ESP8266 wifi(mySerial);

boolean is_lock = true;  // 잠금 상태
boolean is_open = false; // 자물쇠 붙어있음
int open_magnetic_input_pin = 2; 
int open_magnetic_input_val = 0; 

uint8_t buffer[10] = {0};
uint8_t mux_id;
uint32_t len;

void setup(void)
{
    Serial.begin(9600);
    pinMode(IN1_A,OUTPUT);  
    pinMode(IN2_A,OUTPUT);   
    pinMode(open_magnetic_input_pin, INPUT);
    Serial.print("setup begin\r\n");
    
    Serial.print("FW Version:");
    Serial.println(wifi.getVersion().c_str());
      
    if (wifi.setOprToStationSoftAP()) {
        Serial.print("to station + softap ok\r\n");
    } else {
        Serial.print("to station + softap err\r\n");
    }
 
    if (wifi.joinAP(SSID, PASSWORD)) {
        Serial.print("Join AP success\r\n");
        Serial.print("IP: ");
        Serial.println(wifi.getLocalIP().c_str());    
    } else {
        Serial.print("Join AP failure\r\n");
    }
    
    if (wifi.enableMUX()) {
        Serial.print("multiple ok\r\n");
    } else {
        Serial.print("multiple err\r\n");
    }
    
    if (wifi.startTCPServer(8090)) {
        Serial.print("start tcp server ok\r\n");
    } else {
        Serial.print("start tcp server err\r\n");
    }
    
    if (wifi.setTCPServerTimeout(360)) { 
        Serial.print("set tcp server timout 360 seconds\r\n");
    } else {
        Serial.print("set tcp server timout err\r\n");
    }
    
    Serial.print("setup end\r\n");

     pinMode(LED_BUILTIN, OUTPUT);
}

void motor_dir(int dir)  
{  
  if ( dir == 0 ) {   
      // 잠기는 거
     digitalWrite(IN1_A,HIGH);  
     digitalWrite(IN2_A,LOW);  
  
  }  
  else if ( dir == 1 ) { 
      // 열리는 것
     digitalWrite(IN1_A,LOW);  
     digitalWrite(IN2_A,HIGH);  
  }    
}  

void detectMagnetic(){
   // 자물쇠 열렸는지 감지
        open_magnetic_input_val = digitalRead(open_magnetic_input_pin);
        if (open_magnetic_input_val == HIGH) {    
          is_open = false;
          Serial.print("자물쇠 붙어있음: ");
          Serial.println(open_magnetic_input_val);        
          
        } else {
          is_open = true;
          Serial.print("자물쇠 떨어짐: ");
          Serial.println(open_magnetic_input_val);
        }
}

int detectMissing(){
  //   자물쇠 잠금 상태에서 털리면
    if(is_lock){
      if(is_open){
        Serial.println("자물쇠 털림!!!!!! ");
        return 4;
      }
      else{
        Serial.println("자물쇠 잘 있음 ");
        return 7;
      }
    }
    else{
      Serial.println("자물쇠 잘 있음 ");
      return 7;
    }
    
}

void loop(void)
{
  len=wifi.recv(&mux_id,buffer,sizeof(buffer),100);
    
    //delay(100);
     
//    Serial.print("수신받은 명령어 길이: ");
//    Serial.println(len);
//
//    Serial.print("is_lock: ");
//    Serial.println(is_lock);
//    Serial.print("is_open: ");
//    Serial.println(is_open);
       
    if (len > 0) {
        char command = buffer[0];
        Serial.println(command);
        if(command=='5'){
          Serial.println("5야");
          //sprintf(buffer, "7\n");
          //sprintf(buffer, "<조이름: 매트릭스> 아두이노가 1 받고 one을 보냄.\n");
          wifi.send(mux_id, buffer, strlen(buffer));
    
          if(is_lock){
            is_lock=false;
            motor_dir(1);
            delay(5000);
          }
          else{
            is_lock =true;
            motor_dir(0);
            delay(5000);
          }
        }
        else{
          Serial.println("뭔데 그럼");
        }
    }

   detectMagnetic();
   int res = detectMissing();
   Serial.print("res: ");
   Serial.println(res);
    if(res == 4){
      Serial.println("4야");
      sprintf(buffer, "4\n");
    }else{
      sprintf(buffer, "7\n");
      Serial.println("7이야");
    }
    wifi.send(mux_id, buffer, strlen(buffer));
    delay(1000);
}
