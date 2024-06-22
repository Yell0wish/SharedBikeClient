package com.example.sharedbikeclient.ui.charity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sharedbikeclient.R;

import java.util.List;

public class CharityAdapter extends RecyclerView.Adapter<CharityAdapter.CharityViewHolder> {
    private Context context;
    private List<Charity> charityList;

    public CharityAdapter(Context context, List<Charity> charityList) {
        this.context = context;
        this.charityList = charityList;
    }

    @NonNull
    @Override
    public CharityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_charity, parent, false);
        return new CharityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharityViewHolder holder, int position) {
        Charity charity = charityList.get(position);
        holder.textName.setText("姓名：" + charity.getName());
        holder.textGender.setText("性别：" + charity.getGender());
        holder.textBirthdate.setText("出生日期：" + charity.getBirthdate());
        holder.textHeight.setText("失踪时身高：" + charity.getHeight());
        holder.textMissingDate.setText("失踪时间：" + charity.getMissingDate());
        holder.textMissingLocation.setText("失踪地点：" + charity.getMissingLocation());
        holder.textDescription.setText("失踪者特征描述：" + charity.getDescription());
        Glide.with(context).load(charity.getImageUrl()).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return charityList.size();
    }

    public static class CharityViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textName, textGender, textBirthdate, textHeight, textMissingDate, textMissingLocation, textDescription;

        public CharityViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textName = itemView.findViewById(R.id.textName);
            textGender = itemView.findViewById(R.id.textGender);
            textBirthdate = itemView.findViewById(R.id.textBirthdate);
            textHeight = itemView.findViewById(R.id.textHeight);
            textMissingDate = itemView.findViewById(R.id.textMissingDate);
            textMissingLocation = itemView.findViewById(R.id.textMissingLocation);
            textDescription = itemView.findViewById(R.id.textDescription);
        }
    }
}
