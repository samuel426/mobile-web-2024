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
import android.widget.Button;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1; // 이미지 선택을 위한 상수
    private ImageView imgView;
    private TextView textView;
    private RecyclerView recyclerView; // RecyclerView 추가
    private String site_url = "https://samuel26.pythonanywhere.com/";
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

        Button btnDelete = findViewById(R.id.btnDelete); // 삭제 버튼 초기화
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedImage(); // 삭제 기능 호출
            }
        });
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

    // 선택한 이미지를 삭제
    private void deleteSelectedImage() {
        if (imageUri != null) {
            imgView.setImageDrawable(null); // ImageView에서 이미지 삭제
            imageUri = null; // 선택한 이미지 URI 초기화
            Toast.makeText(this, "이미지가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "삭제할 이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    // 비동기적으로 이미지 다운로드 처리
    private class CloadImage extends AsyncTask<String, Integer, List<Post>> {
        @Override
        protected List<Post> doInBackground(String... urls) {
            List<Post> postList = new ArrayList<>();
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
                        String title = post_json.getString("title");
                        String text = post_json.getString("text");

                        Bitmap imageBitmap = null;
                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            imageBitmap = BitmapFactory.decodeStream(imgStream);
                            imgStream.close();
                        }

                        postList.add(new Post(title, text, imageBitmap)); // 포스트 객체 추가
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return postList;
        }

        // UI 업데이트 (이미지 로드 결과)
        @Override
        protected void onPostExecute(List<Post> posts) {
            if (posts.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                ImageAdapter adapter = new ImageAdapter(posts); // RecyclerView 어댑터 설정
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

                // JSON 데이터 전송
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                dos.writeBytes(title + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
                dos.writeBytes(text + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"\r\n\r\n");
                dos.writeBytes(String.valueOf(authorId) + "\r\n");

                // 이미지 바이트 배열 전송
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                dos.write(byteArray);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");

                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseMessage.append(line);
                    }
                    is.close();
                } else {
                    responseMessage.append("Error: ").append(responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "네트워크 오류 발생";
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return responseMessage.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }
}
