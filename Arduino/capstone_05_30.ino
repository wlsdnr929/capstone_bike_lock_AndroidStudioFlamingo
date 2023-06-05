#include "ESP8266.h"
#include <SoftwareSerial.h>
#include <Wire.h>

//#define SSID        "Won"
#define SSID        "TP-Link_A3B2"
//#define SSID        "07302"  
//#define SSID          "how you like that~"
//#define SSID        "Mingu"

//#define PASSWORD    "0730207302" 
//#define PASSWORD      "fire eggs"
//#define PASSWORD    "25801111" 
#define PASSWORD    "60939388" 
//#define PASSWORD    "rnjsalsrn1" 

#define IN1_A  6   
#define IN2_A  5   

byte speedDC = 255;

SoftwareSerial mySerial(10, 9); /* TX:D10, RX:D9 */
ESP8266 wifi(mySerial);

boolean is_lock = true;  // 잠금 상태
boolean is_open = false; // 자물쇠 붙어있음
boolean is_ride = false; // 처음엔 주행중 아님
int open_magnetic_input_pin = 2; 
int open_magnetic_input_val = 0; 

uint8_t buffer[10] = {0};
uint8_t mux_id;
uint32_t len;

int now_status=7;
int change_status;

const int MPU_addr=0x68;  // I2C address of the MPU-6050
const float alpha = 0.9;   // Filter parameter (0 < alpha < 1)

float AcX,AcY,AcZ;
float AcX_offset, AcY_offset, AcZ_offset;
float filter_pitch, filter_roll;
float prev_roll = 0, prev_pitch = 0;

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

    Wire.begin();
    Wire.beginTransmission(MPU_addr);
    Wire.write(0x6B); // PWR_MGMT_1 register
    Wire.write(0);    // set to zero (wakes up the MPU-6050)
    Wire.endTransmission(true);
    
    setOffsets(); // Call the function to set the sensor offsets
    
    Serial.print("setup end\r\n");

     pinMode(LED_BUILTIN, OUTPUT);
}

void motor_dir(int dir)  
{  
  if ( dir == 1 ) {   
      // 열리는 거
     digitalWrite(IN1_A,HIGH);  
     digitalWrite(IN2_A,LOW);  

  }  
  else if ( dir == 0 ) { 
      // 잠기는 것
     digitalWrite(IN1_A,LOW);  
     digitalWrite(IN2_A,HIGH);  
  }    
}  

int cnt = 0, flag = 0;
void loop(void)
{
  len=wifi.recv(&mux_id,buffer,sizeof(buffer),100);

  // Read sensor data
  Wire.beginTransmission(MPU_addr);
  Wire.write(0x3B); // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_addr, 14, true); // request a total of 14 registers


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
//            flag=0;
//            command='20';
            delay(5000);
          }
          else{
            is_lock =true;
            motor_dir(0);
            delay(5000);
          }
        }
        
        // 주행 중/종료
        else if(command == '1'){
          Serial.println("1야");
          Serial.print("is_ride: ");
          Serial.println(is_ride);
         if(is_ride==false){
          // 주행 중으로
          is_ride = true;
         }
         else{
          // 주행 종료
          is_ride = false;
          flag=0;
         }
        }
        // 도난 확인
        else if(command == '9'){
          flag=0;
          command='20';
        }
        else{
          Serial.println("뭔데 그럼");
        }
    }

AcX = Wire.read()<<8|Wire.read(); // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)
  AcY = Wire.read()<<8|Wire.read(); // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
  AcZ = Wire.read()<<8|Wire.read(); // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)

  // Apply sensor offsets
  AcX -= AcX_offset * 16384.0;
  AcY -= AcY_offset * 16384.0;
  AcZ -= (AcZ_offset + 1.0) * 16384.0; // Add 1g back to the Z axis

  // Calculate pitch and roll angles (in degrees)
  float pitch = atan2(AcX, sqrt(AcY*AcY + AcZ*AcZ)) * 180.0 / PI;
  float roll = atan2(AcY, sqrt(AcX*AcX + AcZ*AcZ)) * 180.0 / PI;
  
  Serial.print("Pitch: "); Serial.print(pitch);
  Serial.print(" Roll: "); Serial.print(roll);

  // Apply LPF
  filter_pitch = alpha * pitch + (1 - alpha) * prev_pitch;
  filter_roll = alpha * roll + (1 - alpha) * prev_roll;

  Serial.print(" Filter_Pitch: "); Serial.print(filter_pitch);
  Serial.print(" Filter_Roll: "); Serial.println(filter_roll);
  


  if ((abs(prev_pitch - filter_pitch) > 6.0) || (abs(prev_roll - filter_roll) > 6.0)) {
    flag++;
  }

  prev_pitch = filter_pitch;
  prev_roll = filter_roll;

  // 주행 중이 아니면
  if (flag > 1 && !is_ride) {
    change_status = 4;
    Serial.println("도난 발생 추정!! 확인 바람!!");
    Serial.println("4이야");
    sprintf(buffer, "4\n");
   
  }
  else {
    change_status=7;
    Serial.println("7이야");
    sprintf(buffer, "7\n");
  }

    if(now_status != change_status){
      now_status =change_status;
      wifi.send(mux_id, buffer, strlen(buffer));
    }

    
    
    delay(1000);
}

void setOffsets(){
  int AcX_sum = 0, AcY_sum = 0, AcZ_sum = 0;
  const int numSamples = 100;

  // Take a number of readings and average them to determine the sensor offsets
  for (int i=0; i<numSamples; i++){
    Wire.beginTransmission(MPU_addr);
    Wire.write(0x3B);
    Wire.endTransmission(false);
    Wire.requestFrom(MPU_addr,14,true);

    AcX_sum += Wire.read()<<8|Wire.read(); // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)
    AcY_sum += Wire.read()<<8|Wire.read(); // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
    AcZ_sum += Wire.read()<<8|Wire.read(); // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
    
    delay(10); // Wait a short time between readings
  }

  // Calculate the sensor offsets as the average of the readings
  AcX_offset = (float) AcX_sum/numSamples/16384.0;
  AcY_offset = (float) AcY_sum/numSamples/16384.0;
  AcZ_offset = (float) AcZ_sum/numSamples/16384.0 - 1.0; // Subtract 1g from the Z axis
  
}
