package com.example.eduflow.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.eduflow.R;
import com.example.eduflow.adapters.CategoryAdapter;
import com.example.eduflow.adapters.ExploreVideoAdapter;
import com.example.eduflow.databinding.FragmentExploreBinding;
import com.example.eduflow.models.Category;
import com.example.eduflow.models.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;

    private CategoryAdapter categoryAdapter;
    private ExploreVideoAdapter videoAdapter;

    private List<Category> allCategories = new ArrayList<>();
    private List<Video> allVideos = new ArrayList<>();

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
        setupVideos();
        setupSearch();
        styleSearchView(); // ✅ 让Search文字变白色
    }

    private void setupCategories() {
        allCategories = Arrays.asList(
                new Category("1", "Photography", R.drawable.ic_category_camera, 0),
                new Category("2", "Math", R.drawable.ic_category_math, 0),
                new Category("3", "Programming", R.drawable.ic_category_programming, 0),
                new Category("4", "English", R.drawable.ic_category_languages, 0),
                new Category("5", "Business", R.drawable.ic_category_business, 0),
                new Category("6", "Exercise", R.drawable.ic_category_health, 0)
        );

        categoryAdapter = new CategoryAdapter(allCategories, category -> {
            if (getContext() == null) return;

            android.content.Intent intent = new android.content.Intent(getContext(),
                    com.example.eduflow.CategoryVideosActivity.class);
            intent.putExtra(com.example.eduflow.CategoryVideosActivity.EXTRA_CATEGORY_NAME, category.getName());
            startActivity(intent);
        });

        binding.rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupVideos() {
        // ✅ Explore 列表展示用假数据（后面换成真实数据源）
        allVideos = Arrays.asList(
                new Video("v1", "Intro to Programming", "Kai Yang", "", "", 1200, 45,
                        Arrays.asList("coding", "basics"), "Programming"),
                new Video("v2", "Math in 10 Minutes", "Teoh Jun Ze", "", "", 800, 20,
                        Arrays.asList("algebra"), "Math"),
                new Video("v3", "Business Pitch Tips", "Yong Jeen", "", "", 560, 12,
                        Arrays.asList("presentation"), "Business"),
                new Video("v4", "English Listening Practice", "Sarah", "", "", 430, 9,
                        Arrays.asList("listening"), "English")
        );

        videoAdapter = new ExploreVideoAdapter(allVideos, video -> {
            if (getContext() == null) return;

            // ✅ 点击 Watch -> 进入分类页（你们已有 CategoryVideosActivity）
            android.content.Intent intent = new android.content.Intent(getContext(),
                    com.example.eduflow.CategoryVideosActivity.class);
            intent.putExtra(com.example.eduflow.CategoryVideosActivity.EXTRA_CATEGORY_NAME, video.getCategory());
            startActivity(intent);
        });

        binding.rvVideos.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvVideos.setAdapter(videoAdapter);
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCategories(query);
                filterVideos(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCategories(newText);
                filterVideos(newText);
                return true;
            }
        });
    }

    private void styleSearchView() {
        // ✅ make search text color visible on dark background
        TextView searchText = binding.searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTextColor(getResources().getColor(android.R.color.white));
            searchText.setHintTextColor(0x88FFFFFF);
        }
    }

    private void filterCategories(String query) {
        if (categoryAdapter == null) return;

        List<Category> filteredList;
        if (query == null || query.trim().isEmpty()) {
            filteredList = allCategories;
        } else {
            filteredList = new ArrayList<>();
            String q = query.toLowerCase();
            for (Category category : allCategories) {
                if (category.getName() != null &&
                        category.getName().toLowerCase().contains(q)) {
                    filteredList.add(category);
                }
            }
        }
        categoryAdapter.updateList(filteredList);
    }

    private void filterVideos(String query) {
        if (videoAdapter == null) return;

        List<Video> filtered;
        if (query == null || query.trim().isEmpty()) {
            filtered = allVideos;
        } else {
            filtered = new ArrayList<>();
            String q = query.toLowerCase();

            for (Video v : allVideos) {
                String title = v.getTitle() == null ? "" : v.getTitle();
                String author = v.getAuthor() == null ? "" : v.getAuthor();
                String category = v.getCategory() == null ? "" : v.getCategory();

                String text = (title + " " + author + " " + category).toLowerCase();
                if (text.contains(q)) {
                    filtered.add(v);
                }
            }
        }
        videoAdapter.updateList(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
