package com.example.eduflow.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.eduflow.R;
import com.example.eduflow.adapters.CategoryAdapter;
import com.example.eduflow.databinding.FragmentExploreBinding;
import com.example.eduflow.models.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private CategoryAdapter categoryAdapter;
    private List<Category> allCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupCategories();
        setupSearch();
    }

    private void setupCategories() {
        allCategories = Arrays.asList(
                new Category("1", "Photography", R.drawable.ic_category_camera, 0),
                new Category("2", "Math", R.drawable.ic_category_math, 0),
                new Category("3", "Programming", R.drawable.ic_category_programming, 0),
                new Category("4", "English", R.drawable.ic_category_languages, 0),
                new Category("5", "Business", R.drawable.ic_category_business, 0),
                new Category("6", "Exercise", R.drawable.ic_category_health, 0));

        categoryAdapter = new CategoryAdapter(allCategories, category -> {
            // Navigate to category videos
            android.content.Intent intent = new android.content.Intent(getContext(),
                    com.example.eduflow.CategoryVideosActivity.class);
            intent.putExtra(com.example.eduflow.CategoryVideosActivity.EXTRA_CATEGORY_NAME, category.getName());
            startActivity(intent);
        });

        binding.rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCategories(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCategories(newText);
                return true;
            }
        });
    }

    private void filterCategories(String query) {
        List<Category> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = allCategories;
        } else {
            filteredList = new ArrayList<>();
            for (Category category : allCategories) {
                if (category.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(category);
                }
            }
        }
        categoryAdapter.updateList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
