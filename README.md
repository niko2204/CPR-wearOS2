# CPR-wearOS2
## 참여연구원
 * 이영호, 김선경, 최종명, 박건우, 양효정
 * 목포대학교 재난안전사업단
## 제작 목적
1. 이미지 뷰어를 이용하여 슬라이드 보여주기
2. 각 이미지에 정해진 시간 만큼 보여주고 다음 슬라이드로 넘어가기
3. 정해진 슬라이드가 보여질때 notification 발생시키기 --> 스마트 워치(갤럭시워치4) 진동 발생
4. 110 BPM 비프음 발생
## 타이틀 바 없애기
- style.xml에 다음 추가
```
<item name="windowActionBar">false</item>
<item name="windowNoTitle">true</item>
```

 ## Thread 와 Handler
 ```
 Thread thread;
 boolean isThread = true;
 ```
 onCreat 함수 안에 다음과 같이 thread를 생성한다.
 ```
 thread = new Thread(){
            @Override
            public void run() {
                while(isThread){
                    try {
                        sleep(interval[indexofinterval]);


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(indexofinterval);
                    indexofinterval++;
                }
            }
        };
        thread.start();
 ```
 다음은 handler코드이다.
 ```
 private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            mDemoSlider.moveNextPosition();
            if(vibration[msg.what] == 1) {
             ///   removeNotification();
              //  createNotification();
            }

        }
    };
 ```
다음은 갤럭시워치4의 wearOS에서 진동을 재생하기 위한 코드임.
 ```
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }
 ```
## 시나리오
* (진동)
* 1 번 슬라이드 10초
* (진동)
* 2 번 슬라이드 10초
* (진동)
* 3 번 슬라이드 5초
* (진동)
* 4 번 슬라이드 90초 110/min (속도의 알람음)
* (진동)
* 5 번 슬라이드 15초
* (진동)
* 6 번 슬라이드 5초 
* (진동) 
* 7 번 슬라이드 20초
* (진동)
* 8 번 슬라이드 15초
* (진동)
* 9 번 슬라이드 15초
* (진동)
* 10 번 슬라이드 5초 110/min (속도의 알람음)
* (진동) 
* 11 번 슬라이드 5초
* (진동)
* 12 번 슬라이드 5초
* (진동) 
* 13 번 슬라이드 5초
* (진동) 
* 14 번 슬라이드 60초 110/min (속도의 알람음)
* (진동) 


## 2022년 5월 (새로운 CPR 훈련을 위한 변경)
 * 슬라이드를 시작하는 버튼을 두개 제작. CPR훈련, 자가학습
 * CPR 훈련은 충분한 시간 후 슬라이드 넘어감
 * 자가학습은 기존의 그대로

## 2022년 6월 (갤럭시 워치4를 위한 wearOS 앱 추가)
 * wearOS 앱 개발. 


