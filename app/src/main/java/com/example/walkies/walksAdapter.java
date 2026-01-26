package com.example.walkies;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class walksAdapter extends RecyclerView.Adapter<walksAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<walkModel> walksModelArrayList;

    // Constructor
    public walksAdapter(Context context, ArrayList<walkModel> walksModelArrayList) {
        this.context = context;
        this.walksModelArrayList = walksModelArrayList;
    }

    @NonNull
    @Override
    public walksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // to inflate the layout for each item of recycler view.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull walksAdapter.ViewHolder holder, int position) {
        // to set data to textview of each card layout
        walkModel model = walksModelArrayList.get(position);
        holder.name.setText(model.getWalkName());
        holder.distance.setText(String.format("%.2f miles", model.getWalkDistance()));
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.idWalkName);
            distance = itemView.findViewById(R.id.idDistance);
        }
    }
}
