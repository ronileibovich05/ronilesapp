package com.example.ronilesapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class CategoryTasksPagerAdapter extends FragmentStateAdapter {

    private List<String> categories;

    public CategoryTasksPagerAdapter(@NonNull FragmentActivity fa, List<String> categories) {
        super(fa);
        this.categories = categories;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return CategoryTasksFragment.newInstance(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
}
