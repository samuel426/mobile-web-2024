package com.example.myapplication;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private final List<Post> postList; // Post 리스트

    public ImageAdapter(List<Post> posts) {
        this.postList = posts; // 생성자에서 리스트 초기화
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.imageView.setImageBitmap(post.getImage()); // 비트맵 설정
        holder.textViewTitle.setText(post.getTitle()); // 제목 설정
        holder.textViewText.setText(post.getText()); // 내용 설정
    }

    @Override
    public int getItemCount() {
        return postList.size(); // 리스트 크기 반환
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewTitle;
        TextView textViewText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem); // 이미지뷰 초기화
            textViewTitle = itemView.findViewById(R.id.textViewTitle); // 제목 텍스트뷰 초기화
            textViewText = itemView.findViewById(R.id.textViewText); // 내용 텍스트뷰 초기화
        }
    }
}
