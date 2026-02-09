package com.example.walkies;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class walksAdapter extends RecyclerView.Adapter<walksAdapter.ViewHolder> {

    private final Context context;
    final ArrayList<walkModel> walksModelArrayList;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final OnWalkClickListener listener;

    public interface OnWalkClickListener {
        void onWalkClick(walkModel walk);
        void onRouteButtonClick(walkModel walk);
    }

    // Constructor
    public walksAdapter(Context context, ArrayList<walkModel> walksModelArrayList, OnWalkClickListener listener) {
        this.context = context;
        this.walksModelArrayList = walksModelArrayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public walksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // to inflate the layout for each item of recycler view.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    public void setSelectedWalk(walkModel walk) {
        int newPosition = walksModelArrayList.indexOf(walk);

        if (newPosition == -1) return;

        int oldPosition = selectedPosition;
        selectedPosition = newPosition;

        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition);
        }
        notifyItemChanged(newPosition);
    }



    @Override
    public void onBindViewHolder(@NonNull walksAdapter.ViewHolder holder, int position) {
        // to set data to textview of each card layout
        walkModel model = walksModelArrayList.get(position);
        holder.name.setText(model.getWalkName());
        holder.distance.setText(String.format("%.2f miles", model.getWalkDistance()));

        // Set visibility of the tick based on the selection state
        if (selectedPosition == position) {
            holder.tick.setVisibility(View.VISIBLE);
        } else {
            holder.tick.setVisibility(View.GONE);
        }

        // Handle card click
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            
            if (selectedPosition == position) {
                // If the same item is clicked, deselect it
                selectedPosition = RecyclerView.NO_POSITION;
            } else {
                // Select the new item
                selectedPosition = position;
                if (listener != null) {
                    listener.onWalkClick(model);
                }
            }

            // Refresh the previous and new selection
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition);
            }
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition);
            }
        });

        // Handle button click for starting route
        holder.tick.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteButtonClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        // this method is used for showing number of card items in recycler view
        return walksModelArrayList.size();
    }

    // View holder class for initializing of your views
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView distance;
        final ImageButton tick;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.idWalkName);
            distance = itemView.findViewById(R.id.idDistance);
            tick = itemView.findViewById(R.id.imageButton);
        }
    }
}
