package com.example.walkies.tamagotchi;

import android.content.Context;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkies.circularWalks.CircularWalksMap;
import com.example.walkies.mysteryWalks.MysteryWalks;
import com.example.walkies.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TamagotchiUI {

    private final FragmentActivity activity;

    public TamagotchiUI(FragmentActivity activity) {
        this.activity = activity;
    }

    private static int getDialogWidth(android.content.res.Resources res) {
        TypedValue outValue = new TypedValue();
        res.getValue(R.dimen.dialog_width_percent, outValue, true);
        float percent = outValue.getFloat();
        return (int) (res.getDisplayMetrics().widthPixels * percent);
    }

    public void showWalkOptions() {
        FragmentManager fm = activity.getSupportFragmentManager();
        WalkDialogFragment dialog = new WalkDialogFragment();
        dialog.show(fm, "WalkDialogFragment");
    }

    public void showNightWalkWarning(Runnable onConfirm) {
        FragmentManager fm = activity.getSupportFragmentManager();
        NightWalkWarningFragment dialog = NightWalkWarningFragment.newInstance(onConfirm);
        dialog.show(fm, "NightWalkWarningDialog");
    }

    public void showLevelUpDialog(int newLevel) {
        FragmentManager fm = activity.getSupportFragmentManager();
        LevelUpDialogFragment dialog = LevelUpDialogFragment.newInstance(newLevel);
        dialog.show(fm, "LevelUpDialog");
    }

    public void showCurrentLevelDialog(int level, int xp) {
        FragmentManager fm = activity.getSupportFragmentManager();
        CurrentLevelFragment dialog = CurrentLevelFragment.newInstance(level, xp);
        dialog.show(fm, "CurrentLevelDialog");
    }

    public void showSettingsDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        SettingsDialogFragment dialog = new SettingsDialogFragment();
        dialog.show(fm, "SettingsDialog");
    }

    public void showSettingsDetailsDialog(String username, String city, boolean muted) {
        FragmentManager fm = activity.getSupportFragmentManager();
        SettingsDetailsDialogFragment dialog = SettingsDetailsDialogFragment.newInstance(username, city, muted);
        dialog.show(fm, "SettingsDetailsDialog");
    }

    public void showLifetimeStatsDialog(int xp, int coins, int circular, int mystery, int fed, int bathed) {
        FragmentManager fm = activity.getSupportFragmentManager();
        LifetimeStatsDialogFragment dialog = LifetimeStatsDialogFragment.newInstance(xp, coins, circular, mystery, fed, bathed);
        dialog.show(fm, "LifetimeStatsDialog");
    }

    public void showLeaderboardDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        LeaderboardDialogFragment dialog = new LeaderboardDialogFragment();
        dialog.show(fm, "LeaderboardDialog");
    }

    public void showCantAffordDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        CantAffordDialogFragment dialog = new CantAffordDialogFragment();
        dialog.show(fm, "CantAffordDialog");
    }

    public void showWelcomeDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        WelcomeDialogFragment dialog = new WelcomeDialogFragment();
        dialog.show(fm, "WelcomeDialog");
    }

    public void showOnboardingProfileDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        OnboardingProfileDialogFragment dialog = new OnboardingProfileDialogFragment();
        dialog.show(fm, "OnboardingProfileDialog");
    }

    public void showOnboardingGoalDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        OnboardingGoalDialogFragment dialog = new OnboardingGoalDialogFragment();
        dialog.show(fm, "OnboardingGoalDialog");
    }

    public void showNoInternetDialog(Runnable onRetry) {
        FragmentManager fm = activity.getSupportFragmentManager();
        NoInternetDialogFragment dialog = NoInternetDialogFragment.newInstance(onRetry);
        dialog.show(fm, "NoInternetDialog");
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities == null || (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static class WalkDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
            builder.setMessage(R.string.dialog_choose_walk_type)
                    .setPositiveButton(R.string.circular, (dialog, id) -> {
                        Intent intent = new Intent(getActivity(), CircularWalksMap.class);
                        intent.putExtra("is_fresh_launch", true);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.mystery, (dialog, id) -> {
                        Intent intent = new Intent(getActivity(), MysteryWalks.class);
                        intent.putExtra("is_fresh_launch", true);
                        startActivity(intent);
                    });
            return builder.create();
        }
    }

    public static class NightWalkWarningFragment extends DialogFragment {
        private Runnable onConfirm;

        public static NightWalkWarningFragment newInstance(Runnable onConfirm) {
            NightWalkWarningFragment fragment = new NightWalkWarningFragment();
            fragment.onConfirm = onConfirm;
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
            builder.setMessage(R.string.night_walk_warning)
                    .setPositiveButton(R.string.i_agree_to_be_safe, (dialog, id) -> {
                        if (onConfirm != null) onConfirm.run();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss());
            return builder.create();
        }
    }

    public static class LevelUpDialogFragment extends DialogFragment {

        private static final String ARG_LEVEL = "level";

        public static LevelUpDialogFragment newInstance(int level) {
            LevelUpDialogFragment fragment = new LevelUpDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LEVEL, level);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            assert getArguments() != null;
            int level = getArguments().getInt(ARG_LEVEL);

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_level_up, null);

            TextView levelText = view.findViewById(R.id.levelText);

            String title = getString(R.string.level_up_title);
            String subtitle = getString(R.string.level_up_reached, level);
            String reward = getString(R.string.level_up_reward);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(title);
            sb.setSpan(new RelativeSizeSpan(1.4f), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int start = sb.length();
            sb.append(subtitle);
            sb.append(reward);
            sb.setSpan(new RelativeSizeSpan(0.7f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.NORMAL), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            levelText.setText(sb);

            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                RenderEffect blur = RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP);
                requireActivity().getWindow().getDecorView().setRenderEffect(blur);
            }
        }

        @Override
        public void onDismiss(@NonNull android.content.DialogInterface dialog) {
            super.onDismiss(dialog);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                requireActivity().getWindow().getDecorView().setRenderEffect(null);
            }
        }
    }

    public static class CurrentLevelFragment extends DialogFragment {

        private static final String ARG_LEVEL = "level";
        private static final String ARG_XP = "xp";

        public static CurrentLevelFragment newInstance(int level, int xp) {
            CurrentLevelFragment fragment = new CurrentLevelFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LEVEL, level);
            args.putInt(ARG_XP, xp);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            assert getArguments() != null;
            int level = getArguments().getInt(ARG_LEVEL);
            int xp = getArguments().getInt(ARG_XP);
            int totalxp = 100 + (level * 10);

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_level_up, null);

            TextView levelText = view.findViewById(R.id.levelText);

            String title = getString(R.string.current_level_title, level);
            String subtitle = getString(R.string.current_level_xp, xp, totalxp);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append(title);
            sb.setSpan(new RelativeSizeSpan(1.4f), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int start = sb.length();
            sb.append(subtitle);
            sb.setSpan(new RelativeSizeSpan(0.7f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.NORMAL), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            levelText.setText(sb);

            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                RenderEffect blur = RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP);
                requireActivity().getWindow().getDecorView().setRenderEffect(blur);
            }
        }

        @Override
        public void onDismiss(@NonNull android.content.DialogInterface dialog) {
            super.onDismiss(dialog);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                requireActivity().getWindow().getDecorView().setRenderEffect(null);
            }
        }
    }

    public static class SettingsDialogFragment extends DialogFragment {

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_settings, null);

            view.findViewById(R.id.btnStats).setOnClickListener(v -> {
                if (getActivity() instanceof Tamagotchi) {
                    Tamagotchi activity = (Tamagotchi) getActivity();
                    activity.getPresenter().onLifetimeStatsRequested();
                }
                dismiss();
            });

            view.findViewById(R.id.btnLeaderboard).setOnClickListener(v -> {
                if (getActivity() instanceof Tamagotchi) {
                    Tamagotchi activity = (Tamagotchi) getActivity();
                    activity.getPresenter().onLeaderboardRequested();
                }
                dismiss();
            });

            view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
                if (getActivity() instanceof Tamagotchi) {
                    Tamagotchi activity = (Tamagotchi) getActivity();
                    activity.getPresenter().onSettingsDetailsRequested();
                }
                dismiss();
            });

            view.findViewById(R.id.btnBack).setOnClickListener(v -> dismiss());

            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                RenderEffect blur = RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP);
                requireActivity().getWindow().getDecorView().setRenderEffect(blur);
            }
        }

        @Override
        public void onDismiss(@NonNull android.content.DialogInterface dialog) {
            super.onDismiss(dialog);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                requireActivity().getWindow().getDecorView().setRenderEffect(null);
            }
        }
    }

    public static class SettingsDetailsDialogFragment extends DialogFragment {
        private static final String ARG_USERNAME = "username";
        private static final String ARG_CITY = "city";
        private static final String ARG_MUTED = "muted";

        public static SettingsDetailsDialogFragment newInstance(String username, String city, boolean muted) {
            SettingsDetailsDialogFragment fragment = new SettingsDetailsDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_USERNAME, username);
            args.putString(ARG_CITY, city);
            args.putBoolean(ARG_MUTED, muted);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_settings_details, null);

            Bundle args = getArguments();
            assert args != null;
            String username = args.getString(ARG_USERNAME);
            String currentCity = args.getString(ARG_CITY);
            boolean isMuted = args.getBoolean(ARG_MUTED);

            ((TextView) view.findViewById(R.id.tvUsernameValue)).setText(username);

            SwitchCompat switchMute = view.findViewById(R.id.switchMute);
            switchMute.setChecked(isMuted);

            Spinner spinner = view.findViewById(R.id.spinnerChangeCity);
            List<String> cityList = new ArrayList<>();
            cityList.add(getString(R.string.loading_cities));

            ArrayAdapter<String> adapter = getStringArrayAdapter(cityList);
            spinner.setAdapter(adapter);

            FirebaseFirestore.getInstance().collection("Cities").get().addOnSuccessListener(queryDocumentSnapshots -> {
                cityList.clear();
                int selectedIndex = 0;
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    String cityName = document.getId();
                    cityList.add(cityName);
                    if (cityName.equals(currentCity)) {
                        selectedIndex = cityList.size() - 1;
                    }
                }
                adapter.notifyDataSetChanged();
                spinner.setSelection(selectedIndex);
            });

            view.findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
                if (getActivity() instanceof Tamagotchi) {
                    Tamagotchi activity = (Tamagotchi) getActivity();
                    String newCity = spinner.getSelectedItem().toString();
                    boolean muted = switchMute.isChecked();
                    activity.getPresenter().onSettingsSaved(newCity, muted);
                }
                dismiss();
            });

            view.findViewById(R.id.btnBackSettingsDetails).setOnClickListener(v -> dismiss());

            builder.setView(view);
            return builder.create();
        }

        @NonNull
        private ArrayAdapter<String> getStringArrayAdapter(List<String> cityList) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, cityList) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TypedValue typedValue = new TypedValue();
                    requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                    ((TextView) v).setTextColor(typedValue.data);
                    return v;
                }

                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    TypedValue textColor = new TypedValue();
                    requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, textColor, true);
                    ((TextView) v).setTextColor(textColor.data);

                    TypedValue bgColor = new TypedValue();
                    requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, bgColor, true);
                    v.setBackgroundColor(bgColor.data);
                    return v;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            return adapter;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    public static class LifetimeStatsDialogFragment extends DialogFragment {

        private static final String ARG_XP = "xp";
        private static final String ARG_COINS = "coins";
        private static final String ARG_CIRCULAR = "circular";
        private static final String ARG_MYSTERY = "mystery";
        private static final String ARG_FED = "fed";
        private static final String ARG_BATHED = "bathed";

        public static LifetimeStatsDialogFragment newInstance(int xp, int coins, int circular, int mystery, int fed, int bathed) {
            LifetimeStatsDialogFragment fragment = new LifetimeStatsDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_XP, xp);
            args.putInt(ARG_COINS, coins);
            args.putInt(ARG_CIRCULAR, circular);
            args.putInt(ARG_MYSTERY, mystery);
            args.putInt(ARG_FED, fed);
            args.putInt(ARG_BATHED, bathed);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_lifetime_stats, null);

            Bundle args = getArguments();
            assert args != null;
            ((TextView) view.findViewById(R.id.lifetimeXP)).setText(getString(R.string.lifetime_xp, args.getInt(ARG_XP)));
            ((TextView) view.findViewById(R.id.lifetimeCoins)).setText(getString(R.string.lifetime_coins, args.getInt(ARG_COINS)));
            ((TextView) view.findViewById(R.id.lifetimeCircular)).setText(getString(R.string.circular_walks_stat, args.getInt(ARG_CIRCULAR)));
            ((TextView) view.findViewById(R.id.lifetimeMystery)).setText(getString(R.string.mystery_walks_stat, args.getInt(ARG_MYSTERY)));
            ((TextView) view.findViewById(R.id.lifetimeFed)).setText(getString(R.string.times_fed, args.getInt(ARG_FED)));
            ((TextView) view.findViewById(R.id.lifetimeBathed)).setText(getString(R.string.times_bathed, args.getInt(ARG_BATHED)));

            view.findViewById(R.id.btnBackStats).setOnClickListener(v -> dismiss());

            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                RenderEffect blur = RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP);
                requireActivity().getWindow().getDecorView().setRenderEffect(blur);
            }
        }

        @Override
        public void onDismiss(@NonNull android.content.DialogInterface dialog) {
            super.onDismiss(dialog);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                requireActivity().getWindow().getDecorView().setRenderEffect(null);
            }
        }
    }

    public static class LeaderboardDialogFragment extends DialogFragment {
        private List<LeaderboardEntry> entries;
        private LeaderboardAdapter adapter;
        private RecyclerView rv;
        private ProgressBar pb;
        private String goal;
        private String city;

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_leaderboard, null);

            rv = view.findViewById(R.id.rvLeaderboard);
            pb = view.findViewById(R.id.pbLeaderboard);
            RadioGroup rgTime = view.findViewById(R.id.rgLeaderboardTime);

            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            entries = new ArrayList<>();
            adapter = new LeaderboardAdapter(entries);
            rv.setAdapter(adapter);

            if (getActivity() instanceof Tamagotchi) {
                Tamagotchi activity = (Tamagotchi) getActivity();
                goal = activity.getRepository().getGoal();
                city = activity.getRepository().getCity();

                fetchLeaderboard(true);
            }

            rgTime.setOnCheckedChangeListener((group, checkedId) -> {
                fetchLeaderboard(checkedId == R.id.rbLifetime);
            });

            view.findViewById(R.id.btnBackLeaderboard).setOnClickListener(v -> dismiss());

            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            return dialog;
        }

        private void fetchLeaderboard(boolean isLifetime) {
            pb.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String sortField = isLifetime ? "lifetimeXP" : "monthlyXP";

            Query query;
            if ("friends".equals(goal)) {
                query = db.collection("Users")
                        .whereEqualTo("city", city)
                        .orderBy(sortField, Query.Direction.DESCENDING)
                        .limit(10);
            } else {
                query = db.collection("Users")
                        .orderBy(sortField, Query.Direction.DESCENDING)
                        .limit(10);
            }

            query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                entries.clear();
                int rank = 1;
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String username = doc.getString("username");
                    Long xp = doc.getLong(sortField);
                    Long level = doc.getLong("level");

                    entries.add(new LeaderboardEntry(
                        rank++,
                        username != null ? username : getString(R.string.unknown),
                        level != null ? level.intValue() : 1,
                        xp != null ? xp.intValue() : 0
                    ));
                }
                adapter.notifyDataSetChanged();
                pb.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }).addOnFailureListener(e -> {
                Log.e("Leaderboard", "Error fetching leaderboard", e);
                pb.setVisibility(View.GONE);
                Toast.makeText(requireContext(), getString(R.string.error_loading_leaderboard), Toast.LENGTH_LONG).show();
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private static class LeaderboardEntry {
        int rank;
        String username;
        int level;
        int xp;

        LeaderboardEntry(int rank, String username, int level, int xp) {
            this.rank = rank;
            this.username = username;
            this.level = level;
            this.xp = xp;
        }
    }

    private static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private final List<LeaderboardEntry> entries;

        LeaderboardAdapter(List<LeaderboardEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardEntry entry = entries.get(position);
            holder.tvRank.setText(String.valueOf(entry.rank));
            holder.tvUsername.setText(entry.username);
            holder.tvLevel.setText(holder.itemView.getContext().getString(R.string.leaderboard_lvl_format, entry.level));
            holder.tvXP.setText(holder.itemView.getContext().getString(R.string.leaderboard_xp_format, entry.xp));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvUsername, tvLevel, tvXP;
            ViewHolder(View view) {
                super(view);
                tvRank = view.findViewById(R.id.tvRank);
                tvUsername = view.findViewById(R.id.tvUsername);
                tvLevel = view.findViewById(R.id.tvLevel);
                tvXP = view.findViewById(R.id.tvXP);
            }
        }
    }

    public static class CantAffordDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
            builder.setMessage(R.string.cant_afford)
                    .setPositiveButton(R.string.ok, (dialog, id) -> dismiss());
            return builder.create();
        }
    }

    public static class WelcomeDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(requireActivity(), R.style.OnboardingDialogTheme));
            View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_welcome, null);

            view.findViewById(R.id.btnWelcomeContinue).setOnClickListener(v -> {
                if (getActivity() instanceof Tamagotchi) {
                    ((Tamagotchi) getActivity()).getTamagotchiUI().showOnboardingProfileDialog();
                }
                dismiss();
            });

            builder.setView(view);
            return builder.create();
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    public static class OnboardingProfileDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(requireActivity(), R.style.OnboardingDialogTheme));
            View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_onboarding_profile, null);

            Spinner spinner = view.findViewById(R.id.spinnerCity);
            List<String> cityList = new ArrayList<>();
            cityList.add(getString(R.string.loading_cities));

            ArrayAdapter<String> adapter = getStringArrayAdapter(cityList);
            spinner.setAdapter(adapter);

            if (isNetworkAvailable(requireContext())) {
                view.post(() -> {
                    if (getActivity() instanceof Tamagotchi) {
                        ((Tamagotchi) getActivity()).getTamagotchiUI().showNoInternetDialog(() -> {
                            dismiss();
                            ((Tamagotchi) getActivity()).getTamagotchiUI().showOnboardingProfileDialog();
                        });
                    }
                });
            } else {
                FirebaseFirestore.getInstance().collection("Cities").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    cityList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        cityList.add(document.getId());
                    }
                    if (cityList.isEmpty()) {
                        cityList.add(getString(R.string.no_cities_found));
                    }
                    adapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> {
                    cityList.clear();
                    cityList.add(getString(R.string.error_loading_cities));
                    adapter.notifyDataSetChanged();
                });
            }

            view.findViewById(R.id.btnProfileContinue).setOnClickListener(v -> {
                if (isNetworkAvailable(requireContext())) {
                    if (getActivity() instanceof Tamagotchi) {
                        ((Tamagotchi) getActivity()).getTamagotchiUI().showNoInternetDialog(() -> {
                            dismiss();
                            ((Tamagotchi) getActivity()).getTamagotchiUI().showOnboardingProfileDialog();
                        });
                    }
                    return;
                }

                EditText etUsername = view.findViewById(R.id.etUsername);
                String username = etUsername.getText().toString().trim().toLowerCase();
                String city = spinner.getSelectedItem().toString();

                if (username.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.please_enter_username), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getActivity() instanceof Tamagotchi) {
                    Tamagotchi activity = (Tamagotchi) getActivity();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("Users").document(username).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Toast.makeText(requireContext(), getString(R.string.username_taken), Toast.LENGTH_SHORT).show();
                            } else {
                                Map<String, Object> user = new HashMap<>();
                                user.put("city", city);
                                user.put("username", username);
                                user.put("lifetimeXP", 0);
                                user.put("monthlyXP", 0);
                                user.put("level", 1);

                                db.collection("Users").document(username).set(user).addOnSuccessListener(aVoid -> {
                                    activity.getRepository().saveUsername(username);
                                    activity.getRepository().saveCity(city);

                                    activity.getTamagotchiUI().showOnboardingGoalDialog();
                                    dismiss();
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), getString(R.string.error_creating_user), Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.database_error), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            builder.setView(view);
            return builder.create();
        }

        @NonNull
        private ArrayAdapter<String> getStringArrayAdapter(List<String> cityList) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, cityList) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TypedValue typedValue = new TypedValue();
                    requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                    ((TextView) v).setTextColor(typedValue.data);
                    return v;
                }

                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    TypedValue textColor = new TypedValue();
                    requireContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, textColor, true);
                    ((TextView) v).setTextColor(textColor.data);

                    TypedValue bgColor = new TypedValue();
                    requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, bgColor, true);
                    v.setBackgroundColor(bgColor.data);
                    return v;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            return adapter;
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    public static class OnboardingGoalDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(requireActivity(), R.style.OnboardingDialogTheme));
            View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_onboarding_goal, null);

            view.findViewById(R.id.btnGoalFinish).setOnClickListener(v -> {
                RadioGroup rgGoal = view.findViewById(R.id.rgGoal);
                int checkedId = rgGoal.getCheckedRadioButtonId();
                if (checkedId != -1) {
                    String goalValue = "alone";
                    if (checkedId == R.id.rbFriends) {
                        goalValue = "friends";
                    }

                    if (getActivity() instanceof Tamagotchi) {
                        Tamagotchi activity = (Tamagotchi) getActivity();
                        activity.getRepository().saveGoal(goalValue);
                    }
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), getString(R.string.please_select_goal), Toast.LENGTH_SHORT).show();
                }
            });

            builder.setView(view);
            return builder.create();
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    public static class NoInternetDialogFragment extends DialogFragment {
        private Runnable onRetry;

        public static NoInternetDialogFragment newInstance(Runnable onRetry) {
            NoInternetDialogFragment fragment = new NoInternetDialogFragment();
            fragment.onRetry = onRetry;
            return fragment;
        }

        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(requireActivity(), R.style.OnboardingDialogTheme));
            builder.setTitle(R.string.no_internet_title)
                    .setMessage(R.string.no_internet_message)
                    .setPositiveButton(R.string.retry, (dialog, id) -> {
                        if (onRetry != null) onRetry.run();
                        dismiss();
                    });
            return builder.create();
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(getDialogWidth(getResources()), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }
}
