package com.example.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.database.FocusSession;
import com.example.databinding.ItemFocusSessionBinding;

import java.util.ArrayList;
import java.util.List;

public class FocusSessionAdapter extends RecyclerView.Adapter<FocusSessionAdapter.SessionViewHolder> {

    private List<FocusSession> sessionList = new ArrayList<>();

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFocusSessionBinding binding = ItemFocusSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SessionViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        FocusSession session = sessionList.get(position);
        holder.binding.tvItemDate.setText(session.getDate());
        
        long mins = session.getFocusTime() / 60;
        holder.binding.tvItemTimeDetails.setText(
                session.getStartTime() + " - " + session.getEndTime() + " (" + mins + " mins)"
        );

        int distractions = session.getDistractionCount();
        String apps = session.getDistractionApps();
        if (apps == null || apps.isEmpty()) {
            apps = "None";
        }
        holder.binding.tvItemDistractions.setText("Distractions: " + distractions + " | Apps: " + apps);
        
        int percent = (int) Math.round(session.getFocusPercentage());
        holder.binding.tvItemFocusPercent.setText(percent + "%");
        
        holder.binding.tvItemStatusLabel.setText(session.getFocusStatus().toUpperCase());

        int successColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.success);
        int errorColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.error);

        if ("Completed".equalsIgnoreCase(session.getFocusStatus())) {
            holder.binding.indicatorStatus.setBackgroundColor(successColor);
            holder.binding.tvItemStatusLabel.setTextColor(successColor);
        } else {
            holder.binding.indicatorStatus.setBackgroundColor(errorColor);
            holder.binding.tvItemStatusLabel.setTextColor(errorColor);
        }
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSessions(List<FocusSession> sessions) {
        this.sessionList = sessions;
        notifyDataSetChanged();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        final ItemFocusSessionBinding binding;

        SessionViewHolder(ItemFocusSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
