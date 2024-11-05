package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1; // 이미지 선택을 위한 상수
    private ImageView imgView;
    private TextView textView;
    private RecyclerView recyclerView; // RecyclerView 추가
    private String site_url = "http://10.0.2.2:8000";
    private CloadImage taskDownload;

    private EditText editTextTitle, editTextText; // EditText 추가
    private Uri imageUri; // 선택한 이미지의 URI를 저장할 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ImageView 및 RecyclerView 초기화
        imgView = findViewById(R.id.imgView);
        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView); // recyclerView 연결

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextText = findViewById(R.id.editTextText);
    }

    // 다운로드 버튼 클릭 시 이미지 다운로드 시작
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true); // 이전 작업이 실행 중이면 취소
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/"); // API 엔드포인트로 이미지 요청
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    // 새로운 이미지 게시 버튼 클릭 시 이미지 선택 Activity로 이동
    public void onClickUpload(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 이미지 선택 후 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgView.setImageURI(imageUri); // 선택한 이미지를 ImageView에 표시

            // 비트맵으로 변환
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                // 게시물 업로드 실행
                new PutPost().execute(bitmap); // 선택한 비트맵을 PutPost 작업에 전달
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 처리 중 오류 발생", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 비동기적으로 이미지 다운로드 처리
    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "641ab83796b2582d4ff26009cbad288ace518e69"; // 인증 토큰 설정
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token); // 토큰을 사용한 인증
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;

                    // JSON 데이터 수신 및 변환
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    // 배열 내 모든 이미지 다운로드
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post_json = aryJson.getJSONObject(i);
                        String imageUrl = post_json.getString("image");

                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            bitmapList.add(imageBitmap); // 이미지 리스트에 추가
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return bitmapList;
        }

        // UI 업데이트 (이미지 로드 결과)
        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                ImageAdapter adapter = new ImageAdapter(images); // RecyclerView 어댑터 설정
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private class PutPost extends AsyncTask<Bitmap, String, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            if (bitmaps.length == 0) return "이미지가 선택되지 않았습니다.";

            Bitmap bitmap = bitmaps[0];
            String title = editTextTitle.getText().toString();
            String text = editTextText.getText().toString();
            int authorId = 1; // Django에서 'admin' 사용자의 실제 ID(PK)로 변경
            String token = "641ab83796b2582d4ff26009cbad288ace518e69"; // 실제 토큰으로 변경
            String boundary = "===" + System.currentTimeMillis() + "===";

            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            StringBuilder responseMessage = new StringBuilder();

            try {
                URL url = new URL(site_url + "/api_root/Post/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                // 제목 작성
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n");

                // 내용 작성
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n" + text + "\r\n");

                // 작성자 ID 작성
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"\r\n\r\n" + authorId + "\r\n");

                // 현재 시간으로 created_date 및 published_date 설정
                String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"created_date\"\r\n\r\n" + date + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"published_date\"\r\n\r\n" + date + "\r\n");

                // 이미지 파일 업로드
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageData = byteArrayOutputStream.toByteArray();
                dos.write(imageData);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();

                int responseCode = conn.getResponseCode();
                Log.d("HTTP Response Code", String.valueOf(responseCode));
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        responseMessage.append(line);
                    }
                    reader.close();
                } else {
                    responseMessage.append("서버 오류: ").append(responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                responseMessage.append("오류 발생: ").append(e.getMessage());
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return responseMessage.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }
    }

}
