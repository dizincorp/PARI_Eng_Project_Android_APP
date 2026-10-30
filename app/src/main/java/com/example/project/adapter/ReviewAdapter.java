package com.example.project.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.database.entity.ReviewEntity;
import com.example.project.databinding.ItemReviewBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom RecyclerView Adapter for rendering customer reviews and ratings for a parking spot.
 * Employs View Binding and provides defensive null-handling for all review attributes.
 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<ReviewEntity> reviewList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    /**
     * Default constructor for ReviewAdapter.
     */
    public ReviewAdapter() {
    }

    /**
     * Updates the underlying dataset with newly loaded or cached reviews.
     *
     * @param newReviews List of ReviewEntity items to display
     */
    public void setReviews(@Nullable List<ReviewEntity> newReviews) {
        reviewList.clear();
        if (newReviews != null) {
            reviewList.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewBinding binding = ItemReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewEntity review = reviewList.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    /**
     * ViewHolder for single review cards using View Binding.
     */
    class ReviewViewHolder extends RecyclerView.ViewHolder {

        private final ItemReviewBinding binding;

        /**
         * Constructor initializing with ItemReviewBinding.
         *
         * @param binding View binding instance
         */
        public ReviewViewHolder(@NonNull ItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds ReviewEntity properties to UI views.
         *
         * @param review Review entity to bind
         */
        public void bind(@Nullable ReviewEntity review) {
            if (review == null) {
                return;
            }

            // Author name
            String author = review.getAuthorName();
            if (author != null && !author.trim().isEmpty()) {
                binding.textReviewAuthor.setText(author.trim());
            } else {
                binding.textReviewAuthor.setText("Kierowca");
            }

            // Created Date
            long timestamp = review.getCreatedAtTimestamp();
            if (timestamp > 0) {
                binding.textReviewDate.setText(dateFormat.format(new Date(timestamp)));
                binding.textReviewDate.setVisibility(View.VISIBLE);
            } else {
                binding.textReviewDate.setVisibility(View.GONE);
            }

            // Rating Stars
            binding.ratingReviewBar.setRating(review.getRating());

            // Comment text
            String comment = review.getComment();
            if (comment != null && !comment.trim().isEmpty()) {
                binding.textReviewComment.setText(comment.trim());
                binding.textReviewComment.setVisibility(View.VISIBLE);
            } else {
                binding.textReviewComment.setVisibility(View.GONE);
            }
        }
    }
}
