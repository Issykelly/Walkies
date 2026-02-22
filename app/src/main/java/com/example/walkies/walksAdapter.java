package com.example.walkies;

import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import java.util.*;

public class walksAdapter extends RecyclerView.Adapter<walksAdapter.ViewHolder> {

    private final Context context;
    private final OnWalkClickListener listener;

    private List<walkModel> walks = new ArrayList<>();
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnWalkClickListener {
        void onWalkClick(walkModel walk);
        void onRouteButtonClick(walkModel walk);
    }

    public walksAdapter(Context context, List<walkModel> list, OnWalkClickListener listener) {
        this.context = context;
        this.walks = new ArrayList<>(list);
        this.listener = listener;
        setHasStableIds(true);
    }

    // ---------------- UPDATE DATA (DIFFUTIL) ----------------

    public void updateData(List<walkModel> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(walks, newList));
        walks = new ArrayList<>(newList);
        diff.dispatchUpdatesTo(this);
    }

    // ---------------- SELECTION ----------------

    public void setSelectedWalk(walkModel walk) {
        int newPos = walks.indexOf(walk);
        if (newPos == -1) return;

        int oldPos = selectedPosition;
        selectedPosition = newPos;

        if (oldPos != RecyclerView.NO_POSITION)
            notifyItemChanged(oldPos);

        notifyItemChanged(newPos);
    }

    // ---------------- ADAPTER METHODS ----------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        walkModel model = walks.get(position);

        holder.name.setText(model.getWalkName());
        holder.distance.setText(String.format("%.2f miles", model.getWalkDistance()));

        holder.tick.setVisibility(selectedPosition == position
                ? View.VISIBLE : View.GONE);

        // ---------- CARD CLICK ----------
        holder.itemView.setOnClickListener(v -> {

            int old = selectedPosition;

            if (selectedPosition == position)
                selectedPosition = RecyclerView.NO_POSITION;
            else {
                selectedPosition = position;
                if (listener != null)
                    listener.onWalkClick(model);
            }

            if (old != RecyclerView.NO_POSITION)
                notifyItemChanged(old);

            if (selectedPosition != RecyclerView.NO_POSITION)
                notifyItemChanged(selectedPosition);
        });

        // ---------- ROUTE BUTTON ----------
        holder.tick.setOnClickListener(v -> {
            if (listener != null)
                listener.onRouteButtonClick(model);
        });
    }

    @Override
    public long getItemId(int position) {
        return walks.get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return walks.size();
    }

    // ---------------- VIEW HOLDER ----------------

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, distance;
        ImageButton tick;

        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.idWalkName);
            distance = v.findViewById(R.id.idDistance);
            tick = v.findViewById(R.id.imageButton);
        }
    }

    // ---------------- DIFFUTIL ----------------

    static class DiffCallback extends DiffUtil.Callback {

        private final List<walkModel> oldList;
        private final List<walkModel> newList;

        DiffCallback(List<walkModel> oldList, List<walkModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        public int getOldListSize() { return oldList.size(); }
        public int getNewListSize() { return newList.size(); }

        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getWalkName()
                    .equals(newList.get(newPos).getWalkName());
        }

        public boolean areContentsTheSame(int oldPos, int newPos) {
            walkModel o = oldList.get(oldPos);
            walkModel n = newList.get(newPos);
            return o.getWalkDistance() == n.getWalkDistance();
        }
    }
}