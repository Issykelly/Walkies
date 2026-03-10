package com.example.walkies.tamagotchi;

import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.example.walkies.circularWalks.CircularWalksMap;
import com.example.walkies.mysteryWalks.MysteryWalks;
import com.example.walkies.R;

public class TamagotchiUI {

    private final FragmentActivity activity;

    public TamagotchiUI(FragmentActivity activity) {
        this.activity = activity;
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

    public void showLifetimeStatsDialog(int xp, int coins, int circular, int mystery, int fed, int bathed) {
        FragmentManager fm = activity.getSupportFragmentManager();
        LifetimeStatsDialogFragment dialog = LifetimeStatsDialogFragment.newInstance(xp, coins, circular, mystery, fed, bathed);
        dialog.show(fm, "LifetimeStatsDialog");
    }

    public void showCantAffordDialog() {
        FragmentManager fm = activity.getSupportFragmentManager();
        CantAffordDialogFragment dialog = new CantAffordDialogFragment();
        dialog.show(fm, "CantAffordDialog");
    }

    public static class WalkDialogFragment extends DialogFragment {
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
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

        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            int level = getArguments().getInt(ARG_LEVEL);

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                    requireActivity(),
                    R.style.LevelUpDialogStyle
            );

            View view = requireActivity()
                    .getLayoutInflater()
                    .inflate(R.layout.dialog_level_up, null);

            TextView levelText = view.findViewById(R.id.levelText);

            String title = "You leveled up!\n";
            String subtitle = "you've reached level " + level + "\n";
            String reward = "+100 coins";

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
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
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

            String title = "you're currently level " + level + "!\n";
            String subtitle = "you've got " + xp + "/" + totalxp + " xp\n";

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
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
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

            view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
                // Handle Settings click
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
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
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
            ((TextView) view.findViewById(R.id.lifetimeXP)).setText("Lifetime XP: " + args.getInt(ARG_XP));
            ((TextView) view.findViewById(R.id.lifetimeCoins)).setText("Lifetime Coins: " + args.getInt(ARG_COINS));
            ((TextView) view.findViewById(R.id.lifetimeCircular)).setText("Circular Walks: " + args.getInt(ARG_CIRCULAR));
            ((TextView) view.findViewById(R.id.lifetimeMystery)).setText("Mystery Walks: " + args.getInt(ARG_MYSTERY));
            ((TextView) view.findViewById(R.id.lifetimeFed)).setText("Times Fed: " + args.getInt(ARG_FED));
            ((TextView) view.findViewById(R.id.lifetimeBathed)).setText("Times Bathed: " + args.getInt(ARG_BATHED));

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
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    public static class CantAffordDialogFragment extends DialogFragment {
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
            builder.setMessage(R.string.cant_afford)
                    .setPositiveButton(R.string.ok, (dialog, id) -> dismiss());
            return builder.create();
        }
    }
}
