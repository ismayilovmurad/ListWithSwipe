package com.martiandeveloper.listwithswipe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.martiandeveloper.listwithswipe.model.Product;
import com.martiandeveloper.listwithswipe.R;

import java.util.List;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ProductListViewHolder> {

    private final List<Product> productList;

    public ProductListAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductListViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_product_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductListViewHolder holder, int position) {
        holder.recyclerviewProductItemProductNameMTV.setText(productList.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductListViewHolder extends RecyclerView.ViewHolder {

        private final TextView recyclerviewProductItemProductNameMTV;

        public ProductListViewHolder(View itemView) {
            super(itemView);
            recyclerviewProductItemProductNameMTV = itemView.findViewById(R.id.recyclerview_product_item_productNameMTV);
        }

    }

}
