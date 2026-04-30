package com.example.walkies;

import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import java.util.*;

public class walksAdapter extends RecyclerView.Adapter<walksAdapter.ViewHolder> {

    private final OnWalkClickListener listener;

    private List<walkModel> walks;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnWalkClickListener {
        void onWalkClick(walkModel walk);
        void onRouteButtonClick(walkModel walk);
    }

    public walksAdapter(List<walkModel> list, OnWalkClickListener listener) {
        this.walks = new ArrayList<>(list);
        this.listener = listener;
    }

    public void updateData(List<walkModel> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(walks, newList));
        walks = new ArrayList<>(newList);
        diff.dispatchUpdatesTo(this);
    }

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
        holder.distance.setText(holder.itemView.getContext().getString(R.string.miles_format, model.getWalkDistance()));

        holder.tick.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            int old = selectedPosition;
            selectedPosition = (selectedPosition == currentPos) ? RecyclerView.NO_POSITION : currentPos;

            if (selectedPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onWalkClick(walks.get(currentPos));
            }

            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition);
        });

        holder.tick.setOnClickListener(v -> {
            int currentPos = holder.getBindingAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRouteButtonClick(walks.get(currentPos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return walks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        public final TextView distance;
        public final ImageButton tick;

        public ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.idWalkName);
            distance = v.findViewById(R.id.idDistance);
            tick = v.findViewById(R.id.tick);
        }
    }

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
            return oldList.get(oldPos).getWalkName().equals(newList.get(newPos).getWalkName());
        }

        public boolean areContentsTheSame(int oldPos, int newPos) {
            walkModel o = oldList.get(oldPos);
            walkModel n = newList.get(newPos);

            boolean sameName = o.getWalkName().equals(n.getWalkName());
            boolean sameDistance = o.getWalkDistance() == n.getWalkDistance();
            boolean sameHints = Arrays.equals(o.getHints(), n.getHints());

            return sameName && sameDistance && sameHints;
        }
    }
}
